import uuid
from django.db import models
from django.conf import settings
from core_api.models import TimestampedModel

class DeviceToken(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='device_tokens')
    token = models.CharField(max_length=500, unique=True)
    platform = models.CharField(max_length=20, default='ANDROID')

    class Meta:
        ordering = ['-created_at']

class NotificationLog(TimestampedModel):
    EVENT_CHOICES = [
        ('TOPUP_SUCCESS', 'Topup Success'),
        ('LOW_BALANCE', 'Low Balance Alert'),
        ('BOOKING_CONFIRMED', 'Booking Confirmed'),
        ('SLOT_REMINDER', 'Slot Reminder'),
        ('WAIVER_MISSING', 'Waiver Missing Alert'),
        ('BONUS_EXPIRED', 'Bonus Expired Alert'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='notifications')
    event_type = models.CharField(max_length=30, choices=EVENT_CHOICES)
    title = models.CharField(max_length=255)
    body = models.TextField()
    is_sent = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']
