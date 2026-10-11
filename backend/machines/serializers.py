from rest_framework import serializers

class ActivateMachineSerializer(serializers.Serializer):
    activation_code = serializers.CharField()

class IdentifySerializer(serializers.Serializer):
    qr_token = serializers.CharField()

class ChargeSerializer(serializers.Serializer):
    qr_token = serializers.CharField()
    game_code = serializers.CharField()
    idempotency_key = serializers.CharField()

class RefundSerializer(serializers.Serializer):
    reason = serializers.CharField()

class HeartbeatSerializer(serializers.Serializer):
    status = serializers.CharField()
    software_version = serializers.CharField(required=False, allow_blank=True)
    validator_status = serializers.JSONField(required=False, default=dict)

# Cash Kiosk Session Serializers
class StartCashSessionSerializer(serializers.Serializer):
    qr_token = serializers.CharField()
    idempotency_key = serializers.CharField()

class ProcessNoteSerializer(serializers.Serializer):
    note_value = serializers.IntegerField() # 5, 10, 20, 50, 100, 200, 500
    sequence = serializers.IntegerField()
    idempotency_key = serializers.CharField()

class CloseCashSessionSerializer(serializers.Serializer):
    reason = serializers.CharField()
    notes_total = serializers.IntegerField()
