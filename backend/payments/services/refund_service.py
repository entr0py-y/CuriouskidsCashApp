from django.db import transaction
from rest_framework.exceptions import ValidationError
from payments.models import PaymentOrder, PaymentRefund
from payments.services.payment_service import get_payment_provider
from wallets.services import ledger

def admin_refund_topup(payment_order_id: str, reason: str, admin_id: str) -> PaymentRefund:
    """
    Admin-triggered refund of a top-up to the original payment source.
    Debits the wallet (if balance available) and triggers provider refund API.
    """
    order = PaymentOrder.objects.filter(id=payment_order_id).first()
    if not order or order.status != 'PAID' or not order.provider_payment_id:
        raise ValidationError("Only completed PAID payment orders can be refunded.")

    # Check if already refunded
    if PaymentRefund.objects.filter(payment_order=order).exists():
        raise ValidationError("Payment order has already been refunded.")

    with transaction.atomic():
        # 1. Debit user wallet
        ledger.debit_spend(
            user=order.user,
            amount_paise=order.amount_paise,
            reason='REFUND',
            idempotency_key=f"REFUND_TOPUP:{order.id}",
            reference_id=str(order.id)
        )

        # 2. Call Provider Refund API
        provider = get_payment_provider()
        res = provider.refund_payment(order.provider_payment_id, order.amount_paise, reason)

        # 3. Create Refund Record
        refund = PaymentRefund.objects.create(
            payment_order=order,
            amount_paise=order.amount_paise,
            provider_refund_id=res['provider_refund_id'],
            reason=reason,
            created_by=admin_id
        )

    return refund
