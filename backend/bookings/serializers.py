from rest_framework import serializers
from bookings.models import Product, TimeSlot, WaiverDocument, SignedWaiver, Booking

class ProductSerializer(serializers.ModelSerializer):
    class Meta:
        model = Product
        fields = ['id', 'name', 'type', 'description', 'duration_minutes', 'price_paise', 'age_min', 'age_max']

class TimeSlotSerializer(serializers.ModelSerializer):
    available_seats = serializers.ReadOnlyField()

    class Meta:
        model = TimeSlot
        fields = ['id', 'date', 'start_time', 'end_time', 'capacity', 'available_seats']

class WaiverDocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = WaiverDocument
        fields = ['id', 'version', 'title', 'content_text']

class SignedWaiverSerializer(serializers.ModelSerializer):
    class Meta:
        model = SignedWaiver
        fields = ['id', 'participant_name', 'is_minor', 'guardian_name', 'valid_until', 'created_at']

class SignWaiverRequestSerializer(serializers.Serializer):
    participant_name = serializers.CharField()
    is_minor = serializers.BooleanField(default=False)
    guardian_name = serializers.CharField(required=False, allow_blank=True, default='')
    guardian_phone = serializers.CharField(required=False, allow_blank=True, default='')
    document_id = serializers.CharField()
    signature_data = serializers.CharField()

class BookingItemInputSerializer(serializers.Serializer):
    product_id = serializers.CharField()
    quantity = serializers.IntegerField(default=1)

class BookingParticipantInputSerializer(serializers.Serializer):
    name = serializers.CharField()
    is_minor = serializers.BooleanField(default=False)

class CreateBookingRequestSerializer(serializers.Serializer):
    branch_id = serializers.CharField()
    slot_id = serializers.CharField()
    items = BookingItemInputSerializer(many=True)
    participants = BookingParticipantInputSerializer(many=True)
    idempotency_key = serializers.CharField()
    party_details = serializers.JSONField(required=False, default=None)

class BookingSerializer(serializers.ModelSerializer):
    class Meta:
        model = Booking
        fields = ['id', 'branch', 'slot', 'total_paise', 'status', 'entry_pass_code', 'expires_at', 'created_at']
