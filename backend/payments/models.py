import uuid
from django.db import models
from django.conf import settings
from core_api.models import TimestampedModel

class PaymentOrder(TimestampedModel):
    STATUS_CHOICES = [
        ('CREATED', 'Created'),
        ('PENDING', 'Pending'),
        ('PAID', 'Paid'),
        ('FAILED', 'Failed'),
        ('EXPIRED', 'Expired'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='payment_orders')
    amount_paise = models.BigIntegerField()
    currency = models.CharField(max_length=10, default='INR')
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='CREATED')
    idempotency_key = models.CharField(max_length=255, unique=True)
    provider = models.CharField(max_length=50, default='RAZORPAY')

    # Provider-specific references
    provider_order_id = models.CharField(max_length=255, blank=True, null=True, db_index=True)
    provider_payment_id = models.CharField(max_length=255, blank=True, null=True, unique=True, db_index=True)
    provider_signature = models.CharField(max_length=255, blank=True, null=True)

    raw_webhook_payload = models.JSONField(default=dict, blank=True)
    error_message = models.TextField(blank=True, null=True)
    flagged_for_review = models.BooleanField(default=False)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"PaymentOrder {self.id} - {self.status} - {self.amount_paise} paise"

class PaymentRefund(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    payment_order = models.ForeignKey(PaymentOrder, on_delete=models.PROTECT, related_name='refunds')
    amount_paise = models.BigIntegerField()
    provider_refund_id = models.CharField(max_length=255, unique=True)
    reason = models.TextField()
    created_by = models.CharField(max_length=255)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"PaymentRefund {self.id} - {self.amount_paise} paise"
