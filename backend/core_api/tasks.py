import logging
from celery import shared_task
from django.db import transaction
from core_api.models import OutboxEvent

logger = logging.getLogger(__name__)

@shared_task(bind=True, max_retries=3)
def process_outbox_events(self):
    """
    Celery task skeleton to process Outbox pattern events.
    Picks up unprocessed events and attempts to dispatch them.
    """
    # Select unprocessed events, locking the rows so parallel workers don't double-process
    with transaction.atomic():
        events = OutboxEvent.objects.select_for_update(skip_locked=True).filter(processed=False)[:50]

        for event in events:
            try:
                # Future dispatch logic goes here based on event.event_type
                logger.info(f"Processing outbox event {event.id} of type {event.event_type}")
                event.processed = True
                event.save(update_fields=['processed', 'updated_at'])
            except Exception as exc:
                logger.error(f"Failed to process outbox event {event.id}: {exc}")
                # We do not mark as processed if it fails, allowing retries.
                # Depending on business rules, we might increment a retry counter instead.
