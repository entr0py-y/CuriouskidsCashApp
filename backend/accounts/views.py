from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import AllowAny
from rest_framework.exceptions import ValidationError, Throttled

from accounts.serializers import (
    RequestOtpSerializer, VerifyOtpSerializer, SignupSerializer,
    LoginSerializer, RefreshTokenSerializer
)
from accounts.services import otp, auth

def get_ip(request):
    x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
    if x_forwarded_for:
        return x_forwarded_for.split(',')[0]
    return request.META.get('REMOTE_ADDR')

class RequestOtpView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = RequestOtpSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            v_token = otp.request_otp(
                phone_number=serializer.validated_data['phone'],
                purpose=serializer.validated_data['purpose'],
                ip=get_ip(request),
                device_id=request.data.get('device_id', 'unknown')
            )
            return Response({"message": "OTP requested", "verification_token": v_token})
        except ValueError as e:
            msg = str(e)
            if msg == 'cooldown_active':
                raise Throttled(detail="Please wait 30 seconds before requesting a new OTP.")
            elif msg in ('rate_limit_15m', 'rate_limit_1d'):
                raise Throttled(detail="Too many OTP requests. Please try again later.")
            raise ValidationError(msg)

class VerifyOtpView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = VerifyOtpSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            v_token = otp.verify_otp(
                phone_number=serializer.validated_data['phone'],
                otp=serializer.validated_data['otp'],
                purpose=serializer.validated_data['purpose']
            )
            return Response({"verification_token": v_token})
        except ValueError as e:
            msg = str(e)
            if msg == 'invalid_otp':
                raise ValidationError("Invalid OTP. Please check the code and try again.")
            elif msg == 'max_attempts_reached':
                raise ValidationError("Maximum verification attempts reached. Please request a new OTP.")
            raise ValidationError(msg)

class SignupView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = SignupSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            access, refresh = auth.signup(
                verification_token=serializer.validated_data['verification_token'],
                pin=serializer.validated_data['pin'],
                pin_confirm=serializer.validated_data['pin_confirm'],
                device_data=serializer.validated_data['device']
            )
            return Response({"access_token": access, "refresh_token": refresh})
        except ValueError as e:
            msg = str(e)
            if msg == 'user_exists':
                raise ValidationError("An account with this phone number already exists. Please log in.")
            if msg in ('vtoken_invalid', 'verification_token_expired', 'invalid_verification_token'):
                raise ValidationError("Verification session expired. Please request a new OTP.")
            if msg == 'weak_pin':
                raise ValidationError("Please choose a stronger PIN (no simple sequences or repeated numbers).")
            if msg == 'pin_mismatch':
                raise ValidationError("PINs do not match.")
            raise ValidationError(msg)

class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            access, refresh = auth.login(
                phone_number=serializer.validated_data['phone'],
                pin=serializer.validated_data['pin'],
                device_data=serializer.validated_data['device'],
                ip=get_ip(request)
            )
            return Response({"access_token": access, "refresh_token": refresh})
        except ValueError as e:
            msg = str(e)
            if msg == 'step_up_required':
                return Response({"step_up_required": True}, status=status.HTTP_401_UNAUTHORIZED)
            if msg == 'account_locked':
                return Response({"error": "Account temporarily locked due to too many failed attempts."}, status=status.HTTP_403_FORBIDDEN)
            if msg == 'invalid_credentials':
                raise ValidationError("Invalid phone number or PIN")
            raise ValidationError(msg)

class RefreshTokenView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = RefreshTokenSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            access, refresh = auth.rotate_refresh_token(
                token_str=serializer.validated_data['refresh_token']
            )
            return Response({"access_token": access, "refresh_token": refresh})
        except ValueError as e:
            return Response({"error": str(e)}, status=status.HTTP_401_UNAUTHORIZED)
