import json
import logging
from django.db import transaction, IntegrityError
from django.conf import settings
from rest_framework.exceptions import ValidationError
from payments.models import PaymentOrder
from payments.providers.razorpay_provider import RazorpayProvider
from payments.providers.fake_provider import FakePaymentProvider
from wallets.services import ledger
from profiles.services.profile_service import calculate_age

logger = logging.getLogger(__name__)

def get_payment_provider():
    if getattr(settings, 'TESTING', False) or getattr(settings, 'USE_FAKE_PAYMENT_PROVIDER', False):
        return FakePaymentProvider()
    return RazorpayProvider()

def create_topup_order(user, amount_paise: int, idempotency_key: str) -> dict:
    """
    Validates limits and minors policy, creates PaymentOrder (CREATED) and provider order.
    Returns payload for mobile app SDK.
    """
    # 1. Check idempotency
    existing = PaymentOrder.objects.filter(idempotency_key=idempotency_key).first()
    if existing:
        return _format_order_response(existing, user)

    # 2. Minors policy check
    profile = getattr(user, 'profile', None)
    if profile and profile.date_of_birth and calculate_age(profile.date_of_birth) < 18:
        if not getattr(settings, 'MINORS_CAN_TOPUP', False):
            raise ValidationError("Minor accounts are restricted from self-topups.")

    # 3. Min/Max limits check
    min_paise = getattr(settings, 'WALLET_MIN_TOPUP_PAISE', 100) # 1 INR
    max_paise = getattr(settings, 'WALLET_MAX_TOPUP_PAISE', 1000000) # 10,000 INR
    if amount_paise < min_paise or amount_paise > max_paise:
        raise ValidationError(f"Top-up amount must be between {min_paise // 100} and {max_paise // 100} INR.")

    order = PaymentOrder.objects.create(
        user=user,
        amount_paise=amount_paise,
        currency='INR',
        status='CREATED',
        idempotency_key=idempotency_key,
        provider='RAZORPAY'
    )

    provider = get_payment_provider()
    provider_res = provider.create_order(str(order.id), amount_paise, 'INR')
    order.provider_order_id = provider_res['provider_order_id']
    order.status = 'PENDING'
    order.save()

    return _format_order_response(order, user, provider_res.get('key_id'))

def _format_order_response(order: PaymentOrder, user, key_id: str = None) -> dict:
    profile = getattr(user, 'profile', None)
    return {
        "id": str(order.id),
        "provider_order_id": order.provider_order_id,
        "amount_paise": order.amount_paise,
        "currency": order.currency,
        "status": order.status,
        "key_id": key_id or getattr(settings, 'RAZORPAY_KEY_ID', 'rzp_test_mockkey'),
        "prefill": {
            "phone": user.phone_number,
            "email": profile.email if profile else ""
        }
    }

def process_webhook(raw_body: bytes, signature: str, event_payload: dict) -> dict:
    """
    Webhook handler - SOLE AUTHORITY FOR CREDITING MONEY.
    - Verifies HMAC signature.
    - Idempotent and unique constraint on provider_payment_id.
    - Atomic transaction marking order PAID and invoking ledger.credit_topup.
    """
    provider = get_payment_provider()
    if not provider.verify_webhook_signature(raw_body, signature):
        logger.warning("SECURITY_EVENT: Invalid webhook signature detected.")
        raise ValidationError("Invalid webhook signature.")

    event_type = event_payload.get("event")
    if event_type not in ("payment.captured", "order.paid"):
        return {"status": "IGNORED", "event": event_type}

    payment_entity = event_payload.get("payload", {}).get("payment", {}).get("entity", {})
    provider_order_id = payment_entity.get("order_id")
    provider_payment_id = payment_entity.get("id")
    captured_amount = payment_entity.get("amount")
    currency = payment_entity.get("currency", "INR")

    if not (provider_order_id and provider_payment_id and captured_amount):
        raise ValidationError("Incomplete payment payload.")

    with transaction.atomic():
        # Select for update to prevent concurrent duplicate webhooks
        order = PaymentOrder.objects.select_for_update().filter(provider_order_id=provider_order_id).first()
        if not order:
            logger.error(f"PaymentOrder not found for provider_order_id {provider_order_id}")
            raise ValidationError("ORDER_NOT_FOUND")

        # Amount and currency mismatch check
        if order.amount_paise != captured_amount or order.currency != currency:
            order.flagged_for_review = True
            order.error_message = f"Mismatch: expected {order.amount_paise} {order.currency}, got {captured_amount} {currency}"
            order.save()
            logger.error(f"SECURITY_ALERT: Payment mismatch for order {order.id}")
            return {"status": "FLAGGED", "reason": "AMOUNT_MISMATCH"}

        # Check if already paid
        if order.status == 'PAID' or order.provider_payment_id == provider_payment_id:
            return {"status": "ALREADY_PROCESSED", "order_id": str(order.id)}

        # Set provider payment id and status PAID
        try:
            order.provider_payment_id = provider_payment_id
            order.raw_webhook_payload = event_payload
            order.status = 'PAID'
            order.save()
        except IntegrityError:
            # Duplicate provider_payment_id caught by unique constraint
            return {"status": "ALREADY_PROCESSED", "order_id": str(order.id)}

        # Credit wallet in ledger
        ledger.credit_topup(
            user=order.user,
            amount_paise=order.amount_paise,
            source='GATEWAY',
            idempotency_key=f"RAZORPAY:{provider_payment_id}",
            reference_id=provider_payment_id
        )

    return {"status": "PAID", "order_id": str(order.id), "payment_id": provider_payment_id}

def confirm_checkout(order_id: str, provider_payment_id: str, provider_signature: str) -> dict:
    """
    App success callback. Verifies checkout signature to refresh UI.
    Does NOT credit money by itself.
    """
    order = PaymentOrder.objects.filter(id=order_id).first()
    if not order:
        raise ValidationError("ORDER_NOT_FOUND")

    provider = get_payment_provider()
    is_valid = provider.verify_checkout_signature(
        provider_order_id=order.provider_order_id or '',
        provider_payment_id=provider_payment_id,
        signature=provider_signature
    )

    if not is_valid:
        raise ValidationError("Invalid checkout signature.")

    order.provider_payment_id = provider_payment_id
    order.provider_signature = provider_signature
    order.save()

    return {"status": order.status, "confirmed": True}
