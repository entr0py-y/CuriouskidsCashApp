import uuid
from django.db import models

class TimestampedModel(models.Model):
    """
    Base model that adds created_at and updated_at fields.
    """
    created_at = models.DateTimeField(auto_now_add=True, db_index=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        abstract = True

class UUIDTimestampedModel(TimestampedModel):
    """
    Base model for public-facing entities. Uses UUID as primary key.
    """
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)

    class Meta:
        abstract = True

# Skeleton for reliable background task processing (Outbox Pattern)
class OutboxEvent(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    event_type = models.CharField(max_length=255)
    payload = models.JSONField()
    processed = models.BooleanField(default=False, db_index=True)

    class Meta:
        indexes = [
            models.Index(fields=['processed', 'created_at']),
        ]
