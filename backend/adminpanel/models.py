import uuid
from django.db import models
from django.conf import settings
from profiles.models import Branch
from core_api.models import TimestampedModel

class StaffProfile(TimestampedModel):
    ROLE_CHOICES = [
        ('SUPER_ADMIN', 'Super Admin'),
        ('FINANCE', 'Finance Manager'),
        ('CUSTOMER_CARE', 'Customer Care Representative'),
        ('BRANCH_MANAGER', 'Branch Manager'),
        ('AUDITOR', 'Read-Only Auditor'),
    ]

    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='staff_profile')
    role = models.CharField(max_length=30, choices=ROLE_CHOICES)
    branch = models.ForeignKey(Branch, null=True, blank=True, on_delete=models.SET_NULL, related_name='staff_members')
    is_2fa_enabled = models.BooleanField(default=True)

    def __str__(self):
        return f"Staff {self.user.phone_number} - Role: {self.role}"

class AuditLog(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    staff_user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='audit_actions')
    action_type = models.CharField(max_length=100) # e.g., PII_REVEAL, GOODWILL_CREDIT, USER_SUSPEND
    target_model = models.CharField(max_length=100)
    target_id = models.CharField(max_length=255)
    reason = models.TextField()
    before_json = models.JSONField(default=dict, blank=True)
    after_json = models.JSONField(default=dict, blank=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)

    class Meta:
        ordering = ['-created_at']

    def save(self, *args, **kwargs):
        if not self._state.adding:
            raise ValueError("AuditLog entries are immutable and cannot be updated.")
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValueError("AuditLog entries are immutable and cannot be deleted.")

class MakerCheckerRequest(TimestampedModel):
    STATUS_CHOICES = [
        ('PENDING', 'Pending Approval'),
        ('APPROVED', 'Approved'),
        ('REJECTED', 'Rejected'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    maker = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='maker_requests')
    checker = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.PROTECT, related_name='checker_approvals')
    action_type = models.CharField(max_length=100) # e.g. LARGE_GOODWILL_CREDIT
    target_user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='maker_checker_targets')
    amount_paise = models.BigIntegerField(default=0)
    reason = models.TextField()
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='PENDING')
    rejection_reason = models.TextField(blank=True, null=True)

    class Meta:
        ordering = ['-created_at']

class CustomerSupportNote(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    customer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='support_notes')
    staff = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='notes_authored')
    note = models.TextField()

    class Meta:
        ordering = ['-created_at']
