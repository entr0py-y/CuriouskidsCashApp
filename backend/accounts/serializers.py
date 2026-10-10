import phonenumbers
from rest_framework import serializers

def validate_e164_phone(value):
    try:
        parsed = phonenumbers.parse(value, None)
        if not phonenumbers.is_valid_number(parsed):
            raise serializers.ValidationError("Invalid phone number")
        return phonenumbers.format_number(parsed, phonenumbers.PhoneNumberFormat.E164)
    except phonenumbers.NumberParseException:
        raise serializers.ValidationError("Invalid phone number format")

class RequestOtpSerializer(serializers.Serializer):
    phone = serializers.CharField()
    purpose = serializers.ChoiceField(choices=['SIGNUP', 'LOGIN_NEW_DEVICE', 'PIN_RESET'])

    def validate_phone(self, value):
        return validate_e164_phone(value)

class VerifyOtpSerializer(serializers.Serializer):
    phone = serializers.CharField()
    otp = serializers.CharField(max_length=6)
    purpose = serializers.ChoiceField(choices=['SIGNUP', 'LOGIN_NEW_DEVICE', 'PIN_RESET'])

    def validate_phone(self, value):
        return validate_e164_phone(value)

class DeviceDataSerializer(serializers.Serializer):
    id = serializers.CharField(max_length=255)
    name = serializers.CharField(max_length=255, required=False, allow_blank=True)
    platform = serializers.CharField(max_length=50, required=False, allow_blank=True)
    app_version = serializers.CharField(max_length=50, required=False, allow_blank=True)

class SignupSerializer(serializers.Serializer):
    verification_token = serializers.CharField()
    pin = serializers.CharField(max_length=6)
    pin_confirm = serializers.CharField(max_length=6)
    device = DeviceDataSerializer()

class LoginSerializer(serializers.Serializer):
    phone = serializers.CharField()
    pin = serializers.CharField(max_length=6)
    device = DeviceDataSerializer()

    def validate_phone(self, value):
        return validate_e164_phone(value)

class RefreshTokenSerializer(serializers.Serializer):
    refresh_token = serializers.CharField()
