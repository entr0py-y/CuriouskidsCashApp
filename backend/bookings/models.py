import uuid
from django.db import models
from django.conf import settings
from profiles.models import Branch, FamilyMember
from core_api.models import TimestampedModel

class Product(TimestampedModel):
    TYPE_CHOICES = [
        ('TICKET', 'Entry Ticket'),
        ('PACKAGE', 'Activity Package'),
        ('ADDON', 'Add-on Product'),
        ('BIRTHDAY', 'Birthday Party Package'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    branch = models.ForeignKey(Branch, on_delete=models.CASCADE, related_name='products')
    name = models.CharField(max_length=255)
    type = models.CharField(max_length=20, choices=TYPE_CHOICES, default='TICKET')
    description = models.TextField(blank=True)
    duration_minutes = models.IntegerField(default=60)
    price_paise = models.BigIntegerField()
    age_min = models.IntegerField(default=0)
    age_max = models.IntegerField(default=100)
    is_active = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.name} ({self.branch.name}) - {self.price_paise//100} INR"

class TimeSlot(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    branch = models.ForeignKey(Branch, on_delete=models.CASCADE, related_name='time_slots')
    date = models.DateField()
    start_time = models.TimeField()
    end_time = models.TimeField()
    capacity = models.IntegerField(default=50)
    reserved_count = models.IntegerField(default=0)
    confirmed_count = models.IntegerField(default=0)

    class Meta:
        ordering = ['date', 'start_time']
        unique_together = ('branch', 'date', 'start_time')

    @property
    def available_seats(self):
        return max(0, self.capacity - (self.reserved_count + self.confirmed_count))

    def __str__(self):
        return f"{self.branch.name} - {self.date} {self.start_time}-{self.end_time} ({self.available_seats} left)"

class WaiverDocument(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    branch = models.ForeignKey(Branch, on_delete=models.CASCADE, related_name='waivers')
    version = models.CharField(max_length=20)
    title = models.CharField(max_length=255, default="Liability & Safety Waiver")
    content_text = models.TextField()
    is_active = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']

class SignedWaiver(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='signed_waivers')
    participant_name = models.CharField(max_length=255)
    is_minor = models.BooleanField(default=False)
    guardian_name = models.CharField(max_length=255, blank=True)
    guardian_phone = models.CharField(max_length=20, blank=True)
    document = models.ForeignKey(WaiverDocument, on_delete=models.PROTECT)
    signature_data = models.TextField() # Typed name or base64 drawn image
    valid_until = models.DateField()
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    device_id = models.CharField(max_length=255, blank=True)

    class Meta:
        ordering = ['-created_at']

class Booking(TimestampedModel):
    STATUS_CHOICES = [
        ('RESERVED', 'Reserved (Unpaid)'),
        ('CONFIRMED', 'Confirmed (Paid)'),
        ('CANCELLED', 'Cancelled'),
    ]

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='bookings')
    branch = models.ForeignKey(Branch, on_delete=models.PROTECT)
    slot = models.ForeignKey(TimeSlot, on_delete=models.PROTECT, related_name='bookings')
    total_paise = models.BigIntegerField()
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default='RESERVED')
    idempotency_key = models.CharField(max_length=255, unique=True)
    entry_pass_code = models.CharField(max_length=255, blank=True, null=True)
    expires_at = models.DateTimeField()

    class Meta:
        ordering = ['-created_at']

class BookingItem(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    booking = models.ForeignKey(Booking, on_delete=models.CASCADE, related_name='items')
    product = models.ForeignKey(Product, on_delete=models.PROTECT)
    quantity = models.IntegerField(default=1)
    price_paise = models.BigIntegerField()

class BookingParticipant(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    booking = models.ForeignKey(Booking, on_delete=models.CASCADE, related_name='participants')
    name = models.CharField(max_length=255)
    is_minor = models.BooleanField(default=False)
    family_member = models.ForeignKey(FamilyMember, null=True, blank=True, on_delete=models.SET_NULL)
    waiver = models.ForeignKey(SignedWaiver, null=True, blank=True, on_delete=models.SET_NULL)

class PartyDetails(TimestampedModel):
    booking = models.OneToOneField(Booking, on_delete=models.CASCADE, related_name='party_details')
    child_name = models.CharField(max_length=255)
    child_age = models.IntegerField()
    guest_count = models.IntegerField()
    notes = models.TextField(blank=True)
    deposit_paid_paise = models.BigIntegerField(default=0)
