import logging
from django.db import transaction
from django.db.models import Q
from rest_framework.exceptions import ValidationError, PermissionDenied
from accounts.models import User
from wallets.models import Wallet, LedgerTransaction
from wallets.services import ledger
from adminpanel.models import MakerCheckerRequest, CustomerSupportNote
from adminpanel.services.staff_service import verify_staff_permission, log_audit_action, get_staff_profile

logger = logging.getLogger(__name__)

GOODWILL_CREDIT_THRESHOLD_PAISE = 50000 # ₹500 INR

def mask_phone(phone: str) -> str:
    if len(phone) >= 10:
        return f"{phone[:3]}******{phone[-2:]}"
    return "*******"

def search_customers(staff_user, query: str) -> list:
    verify_staff_permission(staff_user, 'VIEW_CUSTOMER')

    users = User.objects.filter(
        Q(phone_number__icontains=query) |
        Q(profile__full_name__icontains=query) |
        Q(id__icontains=query)
    ).distinct()[:20]

    results = []
    for u in users:
        profile = getattr(u, 'profile', None)
        wallet = Wallet.objects.filter(user=u).first()
        results.append({
            "id": str(u.id),
            "phone_masked": mask_phone(u.phone_number),
            "full_name": profile.full_name if profile else "Customer",
            "is_active": u.is_active,
            "wallet_balance_paise": (wallet.main_balance + wallet.bonus_balance) if wallet else 0
        })

    return results

def reveal_pii(staff_user, customer_id: str, reason: str, ip: str) -> dict:
    verify_staff_permission(staff_user, 'PII_REVEAL')
    customer = User.objects.filter(id=customer_id).first()
    if not customer:
        raise ValidationError("CUSTOMER_NOT_FOUND")

    # Immutable PII reveal audit log
    log_audit_action(
        staff_user=staff_user,
        action_type="PII_REVEAL",
        target_model="User",
        target_id=customer_id,
        reason=reason,
        ip=ip
    )

    profile = getattr(customer, 'profile', None)
    return {
        "id": str(customer.id),
        "unmasked_phone": customer.phone_number,
        "email": profile.email if profile else "",
        "full_name": profile.full_name if profile else ""
    }

def adjust_wallet_goodwill(staff_user, customer_id: str, amount_paise: int, reason: str, ip: str) -> dict:
    verify_staff_permission(staff_user, 'GOODWILL_CREDIT_SMALL')
    customer = User.objects.filter(id=customer_id).first()
    if not customer:
        raise ValidationError("CUSTOMER_NOT_FOUND")

    staff_profile = get_staff_profile(staff_user)

    # Maker-Checker rule: If amount > ₹500 and role is CUSTOMER_CARE, requires Finance approval
    if amount_paise > GOODWILL_CREDIT_THRESHOLD_PAISE and staff_profile.role == 'CUSTOMER_CARE':
        req = MakerCheckerRequest.objects.create(
            maker=staff_user,
            action_type="LARGE_GOODWILL_CREDIT",
            target_user=customer,
            amount_paise=amount_paise,
            reason=reason,
            status='PENDING'
        )
        return {"status": "MAKER_CHECKER_REQUIRED", "request_id": str(req.id)}

    # Directly credit wallet for small amounts or authorized roles
    txn = ledger.credit_topup(
        user=customer,
        amount_paise=amount_paise,
        source='GATEWAY', # Goodwill credit
        idempotency_key=f"GOODWILL:{staff_user.id}:{customer_id}:{amount_paise}:{reason[:20]}",
        reference_id=f"STAFF:{staff_user.id}"
    )

    log_audit_action(
        staff_user=staff_user,
        action_type="GOODWILL_CREDIT",
        target_model="Wallet",
        target_id=str(customer.id),
        reason=reason,
        ip=ip,
        after={"amount_paise": amount_paise, "transaction_id": str(txn.id)}
    )

    return {"status": "CREDITED", "transaction_id": str(txn.id)}

def approve_maker_checker_request(checker_user, request_id: str, is_approved: bool, reason: str, ip: str) -> dict:
    verify_staff_permission(checker_user, 'MAKER_CHECKER_APPROVE')

    req = MakerCheckerRequest.objects.filter(id=request_id, status='PENDING').first()
    if not req:
        raise ValidationError("REQUEST_NOT_FOUND")

    # MAKER-CHECKER CANNOT BE SELF-APPROVED!
    if req.maker.id == checker_user.id:
        raise PermissionDenied("SELF_APPROVAL_PROHIBITED: Maker and Checker must be different staff members.")

    with transaction.atomic():
        if is_approved:
            req.status = 'APPROVED'
            req.checker = checker_user
            req.save()

            txn = ledger.credit_topup(
                user=req.target_user,
                amount_paise=req.amount_paise,
                source='GATEWAY',
                idempotency_key=f"MAKER_CHECKER_CREDIT:{req.id}",
                reference_id=f"CHECKER:{checker_user.id}"
            )

            log_audit_action(
                staff_user=checker_user,
                action_type="MAKER_CHECKER_APPROVED",
                target_model="MakerCheckerRequest",
                target_id=str(req.id),
                reason=reason,
                ip=ip,
                after={"status": "APPROVED", "transaction_id": str(txn.id)}
            )
            return {"status": "APPROVED", "transaction_id": str(txn.id)}
        else:
            req.status = 'REJECTED'
            req.checker = checker_user
            req.rejection_reason = reason
            req.save()

            log_audit_action(
                staff_user=checker_user,
                action_type="MAKER_CHECKER_REJECTED",
                target_model="MakerCheckerRequest",
                target_id=str(req.id),
                reason=reason,
                ip=ip
            )
            return {"status": "REJECTED"}

def toggle_user_suspension(staff_user, customer_id: str, suspend: bool, reason: str, ip: str) -> dict:
    verify_staff_permission(staff_user, 'SUSPEND_USER')
    customer = User.objects.filter(id=customer_id).first()
    if not customer:
        raise ValidationError("CUSTOMER_NOT_FOUND")

    customer.is_active = not suspend
    customer.save()

    log_audit_action(
        staff_user=staff_user,
        action_type="USER_SUSPEND" if suspend else "USER_UNSUSPEND",
        target_model="User",
        target_id=customer_id,
        reason=reason,
        ip=ip
    )

    return {"status": "SUSPENDED" if suspend else "ACTIVE", "customer_id": customer_id}
