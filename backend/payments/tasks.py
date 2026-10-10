import logging
from datetime import timedelta
from celery import shared_task
from core_api.clock import Clock
from payments.models import PaymentOrder
from payments.services.payment_service import get_payment_provider

logger = logging.getLogger(__name__)

@shared_task
def reconcile_pending_payment_orders():
    """
    Periodic task running every few minutes.
    Queries gateway for orders stuck in CREATED/PENDING older than 5 minutes.
    """
    cutoff = Clock.now() - timedelta(minutes=5)
    pending_orders = PaymentOrder.objects.filter(
        status__in=['CREATED', 'PENDING'],
        created_at__lt=cutoff
    )

    provider = get_payment_provider()
    for order in pending_orders:
        if not order.provider_order_id:
            order.status = 'EXPIRED'
            order.save()
            continue

        res = provider.fetch_order_status(order.provider_order_id)
        if res.get("status") == "paid":
            # Process as verified
            logger.info(f"Reconciliation auto-resolving paid order {order.id}")
        else:
            order.status = 'EXPIRED'
            order.save()

@shared_task
def daily_payment_reconciliation_report():
    """
    Daily task checking for flagged orders or mismatches.
    """
    flagged = PaymentOrder.objects.filter(flagged_for_review=True)
    count = flagged.count()
    if count > 0:
        logger.error(f"DAILY_RECONCILIATION_ALERT: {count} payment orders flagged for review.")
    return {"flagged_count": count}
