import jwt
from django.conf import settings
from rest_framework import authentication
from rest_framework.exceptions import AuthenticationFailed
from accounts.models import User, Device
from core_api.clock import Clock

class JWTAuthentication(authentication.BaseAuthentication):
    def authenticate(self, request):
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return None

        token_str = auth_header.split(' ')[1]
        try:
            unverified_headers = jwt.get_unverified_header(token_str)
            key = settings.JWT_KEYS.get(unverified_headers.get('kid'))
            if not key:
                raise AuthenticationFailed('invalid_key')

            payload = jwt.decode(token_str, key, algorithms=['HS256'])
            if payload.get('type') != 'access':
                raise AuthenticationFailed('invalid_token_type')

            # Check expiry against our mockable Clock
            if payload.get('exp') < int(Clock.now().timestamp()):
                raise AuthenticationFailed('token_expired')

            user = User.objects.get(id=payload['sub'], is_active=True)
            device = Device.objects.get(id=payload['device_id'], user=user)

        except (jwt.PyJWTError, User.DoesNotExist, Device.DoesNotExist):
            raise AuthenticationFailed('invalid_token')

        request.device = device
        return (user, token_str)
