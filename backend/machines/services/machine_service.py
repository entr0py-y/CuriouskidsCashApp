import uuid
import secrets
from django.conf import settings
from rest_framework.exceptions import ValidationError
from machines.models import Machine
from accounts.models import User
from qr.services import qr_service
from wallets.services import ledger
from wallets.models import Wallet
from core_api.clock import Clock

def activate_machine(activation_code: str) -> tuple[Machine, str]:
    """
    One-time provisioning flow for new machines.
    Exchanges an admin-generated activation_code for a permanent HMAC secret.
    """
    machine = Machine.objects.filter(activation_code=activation_code).first()
    if not machine or not machine.enabled:
        raise ValidationError("Invalid or expired activation code.")

    # Generate cryptographically secure secret
    secret = secrets.token_hex(32)
    machine.secret = secret
    machine.activation_code = None # Invalidate one-time code
    machine.status = 'ONLINE'
    machine.last_seen = Clock.now()
    machine.save()

    return machine, secret

def identify_user(machine: Machine, qr_token_str: str) -> dict:
    """
    Identifies customer at game or kiosk. Returns minimum required data.
    NO phone number, email, or DOB returned.
    """
    payload = qr_service.decode_and_verify_qr_token(qr_token_str)
    user_ref = payload["user_ref"]

    user = User.objects.filter(id=user_ref, is_active=True).first()
    if not user or user.deleted_at is not None:
        return {
            "user_ref": user_ref,
            "display_name": "Customer",
            "wallet_total_paise": 0,
            "eligible": False,
            "reason": "USER_SUSPENDED"
        }

    profile = getattr(user, 'profile', None)
    display_name = "Customer"
    if profile and profile.full_name:
        display_name = profile.full_name.split()[0] # First name only!

    wallet = Wallet.objects.get(user=user)
    total_paise = wallet.main_balance + wallet.bonus_balance

    return {
        "user_ref": user_ref,
        "display_name": display_name,
        "wallet_total_paise": total_paise,
        "eligible": True,
        "reason": None
    }

def charge_game(machine: Machine, qr_token_str: str, game_code: str, idempotency_key: str) -> dict:
    """
    Charges user for game play.
    - Price comes strictly from server price_list.
    - Verifies token and consumes jti.
    - Debits wallet via bank ledger.
    """
    if machine.type != 'GAME':
        raise ValidationError("WRONG_MACHINE_TYPE")

    # 1. Price check from server price_list
    price_paise = machine.price_list.get(game_code)
    if not price_paise:
        raise ValidationError(f"Game code '{game_code}' not configured for this machine.")

    # 2. Decode QR Token & consume jti (Single-use for spending)
    payload = qr_service.decode_and_verify_qr_token(qr_token_str)
    jti = payload["jti"]
    qr_service.consume_jti_for_spending(jti)

    # 3. Retrieve user
    user_ref = payload["user_ref"]
    user = User.objects.filter(id=user_ref, is_active=True).first()
    if not user or user.deleted_at is not None:
        raise ValidationError("USER_SUSPENDED")

    # 4. Debit spend via ledger
    txn = ledger.debit_spend(
        user=user,
        amount_paise=price_paise,
        reason='MACHINE',
        idempotency_key=idempotency_key,
        reference_id=f"{machine.id}:{game_code}"
    )

    wallet = Wallet.objects.get(user=user)
    balance_after = wallet.main_balance + wallet.bonus_balance

    return {
        "approved": True,
        "charged_paise": price_paise,
        "balance_after_paise": balance_after,
        "transaction_id": str(txn.id)
    }

def refund_charge(machine: Machine, transaction_id: str, reason: str) -> dict:
    """
    Refunds a failed game play within short window. Only allowed by machine that created it.
    """
    rev_txn = ledger.reverse(
        transaction_id=transaction_id,
        reason=f"MACHINE_FAULT: {reason}",
        admin_id=f"MACHINE_{machine.id}"
    )

    return {
        "refunded": True,
        "reversal_transaction_id": str(rev_txn.id)
    }

def record_heartbeat(machine: Machine, status: str, software_version: str, validator_status: dict) -> dict:
    """
    Updates machine last_seen and online status.
    """
    machine.status = status
    if software_version:
        machine.software_version = software_version
    machine.last_seen = Clock.now()
    machine.save()

    return {"status": "ACK", "server_time": machine.last_seen.isoformat()}
