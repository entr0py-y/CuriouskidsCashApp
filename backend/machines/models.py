import uuid
from django.db import models
from django.conf import settings
from profiles.models import Branch
from core_api.models import TimestampedModel

class Machine(TimestampedModel):
    TYPE_CHOICES = [
        ('GAME', 'Game Machine'),
        ('VENDING_CASH', 'Cash Kiosk'),
        ('GATE', 'Turnstile Gate'),
    ]

    STATUS_CHOICES = [
        ('ONLINE', 'Online'),
        ('OFFLINE', 'Offline'),
        ('MAINTENANCE', 'Maintenance'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    type = models.CharField(max_length=20, choices=TYPE_CHOICES)
    branch = models.ForeignKey(Branch, on_delete=models.PROTECT, related_name='machines')
    name = models.CharField(max_length=255)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='OFFLINE')
    enabled = models.BooleanField(default=True)
    last_seen = models.DateTimeField(null=True, blank=True)
    software_version = models.CharField(max_length=50, blank=True)

    # Credentials & Security
    secret = models.CharField(max_length=255, blank=True)
    activation_code = models.CharField(max_length=50, blank=True, unique=True, null=True)

    # Server-side Price List
    price_list = models.JSONField(default=dict)

    class Meta:
        ordering = ['-created_at']

    @property
    def is_authenticated(self):
        return True

    def __str__(self):
        return f"{self.name} ({self.type}) at {self.branch.name}"

class CashSession(TimestampedModel):
    STATUS_CHOICES = [
        ('ACTIVE', 'Active'),
        ('CLOSED', 'Closed'),
        ('EXPIRED', 'Expired'),
        ('FLAGGED', 'Flagged Mismatch'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    machine = models.ForeignKey(Machine, on_delete=models.PROTECT, related_name='cash_sessions')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='cash_sessions')
    idempotency_key = models.CharField(max_length=255, unique=True)
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='ACTIVE')
    expires_at = models.DateTimeField()

    notes_total_kiosk = models.BigIntegerField(default=0)
    notes_total_backend_paise = models.BigIntegerField(default=0)
    close_reason = models.CharField(max_length=50, blank=True, null=True)
    flagged_for_review = models.BooleanField(default=False)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"CashSession {self.id} - {self.machine.name} - {self.status}"

class CashSessionNote(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    cash_session = models.ForeignKey(CashSession, on_delete=models.CASCADE, related_name='notes')
    sequence = models.IntegerField()
    note_value = models.IntegerField() # In Rupees: 5, 10, 20, 50, 100, 200, 500
    idempotency_key = models.CharField(max_length=255, unique=True)
    transaction_id = models.CharField(max_length=255, blank=True, null=True)

    class Meta:
        ordering = ['sequence']
        unique_together = ('cash_session', 'sequence')

    def __str__(self):
        return f"Note {self.note_value} INR (Seq {self.sequence}) - Session {self.cash_session.id}"
