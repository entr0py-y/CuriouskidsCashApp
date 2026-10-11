from rest_framework import serializers

class RegisterDeviceTokenSerializer(serializers.Serializer):
    token = serializers.CharField(max_length=500)
    platform = serializers.CharField(default='ANDROID')
