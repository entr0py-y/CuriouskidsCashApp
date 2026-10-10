import uuid
from django.db import models
from django.conf import settings
from core_api.models import TimestampedModel

class Branch(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=255)
    address = models.TextField()
    latitude = models.DecimalField(max_digits=9, decimal_places=6)
    longitude = models.DecimalField(max_digits=9, decimal_places=6)
    opening_hours = models.JSONField(default=dict)
    contact_numbers = models.JSONField(default=list)
    facilities = models.JSONField(default=list)
    images = models.JSONField(default=list)
    is_active = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return self.name

class Profile(TimestampedModel):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='profile')
    full_name = models.CharField(max_length=255, blank=True)
    date_of_birth = models.DateField(null=True, blank=True)
    email = models.EmailField(blank=True)
    gender = models.CharField(max_length=20, blank=True)
    city = models.CharField(max_length=100, blank=True)
    preferred_branch = models.ForeignKey(Branch, null=True, blank=True, on_delete=models.SET_NULL)
    emergency_contact_name = models.CharField(max_length=255, blank=True)
    emergency_contact_phone = models.CharField(max_length=20, blank=True)
    profile_photo_url = models.URLField(blank=True)
    language_preference = models.CharField(max_length=5, default='en')
    notification_preferences = models.JSONField(default=dict)

    # Minor / Guardian Fields
    guardian_name = models.CharField(max_length=255, blank=True)
    guardian_phone = models.CharField(max_length=20, blank=True)
    guardian_relationship = models.CharField(max_length=50, blank=True)
    parental_consent_given = models.BooleanField(default=False)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"Profile of {self.user.phone_number}"

class FamilyMember(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    parent_user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='family_members')
    name = models.CharField(max_length=255)
    date_of_birth = models.DateField()
    profile_photo_url = models.URLField(blank=True)
    promoted_user = models.OneToOneField(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL, related_name='promoted_from_family')

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.name} (Child of {self.parent_user.phone_number})"

class ConsentLedger(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='consents')
    document_type = models.CharField(max_length=50) # TERMS, PRIVACY_POLICY, MARKETING, GUARDIAN_CONSENT
    document_version = models.CharField(max_length=20)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    device_id = models.CharField(max_length=255, blank=True)

    class Meta:
        ordering = ['-created_at']

    def save(self, *args, **kwargs):
        if not self._state.adding:
            raise ValueError("ConsentLedger records are append-only and cannot be updated.")
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise ValueError("ConsentLedger records are append-only and cannot be deleted.")

class ProfileAuditLog(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='profile_audit_logs')
    field_changed = models.CharField(max_length=50)
    old_value = models.TextField(blank=True, null=True)
    new_value = models.TextField(blank=True, null=True)
    changed_by = models.CharField(max_length=255)

    class Meta:
        ordering = ['-created_at']
