from rest_framework import serializers

class ActivateMachineSerializer(serializers.Serializer):
    activation_code = serializers.CharField()

class IdentifySerializer(serializers.Serializer):
    qr_token = serializers.CharField()

class ChargeSerializer(serializers.Serializer):
    qr_token = serializers.CharField()
    game_code = serializers.CharField()
    idempotency_key = serializers.CharField()
    # Price in request body is intentionally ignored per security spec

class RefundSerializer(serializers.Serializer):
    reason = serializers.CharField()

class HeartbeatSerializer(serializers.Serializer):
    status = serializers.CharField()
    software_version = serializers.CharField(required=False, allow_blank=True)
    validator_status = serializers.JSONField(required=False, default=dict)
