import uuid
from django.db import models
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
