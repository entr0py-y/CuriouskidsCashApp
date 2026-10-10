from rest_framework import serializers

class QrTokenResponseSerializer(serializers.Serializer):
    token = serializers.CharField()
    expires_at = serializers.CharField()
