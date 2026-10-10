import jwt
import hashlib
from datetime import timedelta
from django.conf import settings
from accounts.models import User, Device, RefreshTokenFamily, RefreshToken, UserLockout
from accounts.services.pin import is_weak_pin
from core_api.clock import Clock
from django.core.cache import cache
from accounts.signals import user_created

def check_lockout(phone_number: str):
    lockout = UserLockout.objects.filter(phone_number=phone_number).first()
    if lockout and lockout.locked_until and lockout.locked_until > Clock.now():
        raise ValueError("account_locked")

def record_failed_login(phone_number: str, ip: str):
    lockout, _ = UserLockout.objects.get_or_create(phone_number=phone_number)
    lockout.failed_attempts += 1
    if lockout.failed_attempts >= 5:
        multiplier = 2 ** (lockout.failed_attempts - 5)
        minutes = min(15 * multiplier, 1440)
        lockout.locked_until = Clock.now() + timedelta(minutes=minutes)
    lockout.ip_address = ip
    lockout.save()

def clear_lockout(phone_number: str):
    UserLockout.objects.filter(phone_number=phone_number).delete()

def generate_tokens(user, device):
    now = Clock.now()
    access_exp = now + timedelta(minutes=15)
    refresh_exp = now + timedelta(days=30)

    access_payload = {
        'sub': str(user.id),
        'device_id': str(device.id),
        'exp': int(access_exp.timestamp()),
        'iat': int(now.timestamp()),
        'type': 'access'
    }
    access_token = jwt.encode(access_payload, settings.JWT_KEYS[settings.JWT_ACTIVE_KID], algorithm='HS256', headers={'kid': settings.JWT_ACTIVE_KID})

    family = RefreshTokenFamily.objects.create(user=user, device=device)
    refresh_payload = {
        'sub': str(user.id),
        'family_id': str(family.id),
        'exp': int(refresh_exp.timestamp()),
        'iat': int(now.timestamp()),
        'type': 'refresh'
    }
    refresh_token = jwt.encode(refresh_payload, settings.JWT_KEYS[settings.JWT_ACTIVE_KID], algorithm='HS256', headers={'kid': settings.JWT_ACTIVE_KID})

    token_hash = hashlib.sha256(refresh_token.encode()).hexdigest()
    RefreshToken.objects.create(family=family, token_hash=token_hash, expires_at=refresh_exp)

    return access_token, refresh_token

def signup(verification_token: str, pin: str, pin_confirm: str, device_data: dict):
    if pin != pin_confirm:
        raise ValueError("pin_mismatch")

    vdata = cache.get(f"vtoken_{verification_token}")
    if not vdata or vdata["purpose"] != "SIGNUP":
        raise ValueError("invalid_verification_token")

    phone_number = vdata["phone"]
    if is_weak_pin(pin, phone_number):
        raise ValueError("weak_pin")

    if User.objects.filter(phone_number=phone_number).exists():
        raise ValueError("user_exists")

    user = User.objects.create_user(phone_number=phone_number, pin=pin)
    cache.delete(f"vtoken_{verification_token}")

    device, _ = Device.objects.get_or_create(user=user, device_id=device_data['id'], defaults={
        'name': device_data.get('name', ''),
        'platform': device_data.get('platform', ''),
        'app_version': device_data.get('app_version', ''),
        'is_trusted': True
    })

    # Trigger hook for wallet creation
    user_created.send(sender=User, user=user)

    return generate_tokens(user, device)

def login(phone_number: str, pin: str, device_data: dict, ip: str):
    check_lockout(phone_number)
    user = User.objects.filter(phone_number=phone_number, is_active=True).first()

    if not user or not user.check_password(pin):
        record_failed_login(phone_number, ip)
        raise ValueError("invalid_credentials")

    clear_lockout(phone_number)

    device, created = Device.objects.get_or_create(user=user, device_id=device_data['id'], defaults={
        'name': device_data.get('name', ''),
        'platform': device_data.get('platform', ''),
        'app_version': device_data.get('app_version', ''),
        'is_trusted': False
    })

    if not device.is_trusted:
        raise ValueError("step_up_required")

    return generate_tokens(user, device)

def rotate_refresh_token(token_str: str):
    try:
        unverified_headers = jwt.get_unverified_header(token_str)
        kid = unverified_headers.get('kid')
        key = settings.JWT_KEYS.get(kid)
        if not key:
            raise ValueError("invalid_key")

        payload = jwt.decode(token_str, key, algorithms=['HS256'])
        if payload.get('type') != 'refresh':
            raise ValueError("invalid_token_type")
    except jwt.PyJWTError:
        raise ValueError("invalid_token")

    family_id = payload.get('family_id')
    token_hash = hashlib.sha256(token_str.encode()).hexdigest()

    try:
        family = RefreshTokenFamily.objects.get(id=family_id)
    except RefreshTokenFamily.DoesNotExist:
        raise ValueError("invalid_token")

    token_record = RefreshToken.objects.filter(family=family, token_hash=token_hash).first()
    if not token_record:
        raise ValueError("invalid_token")

    if token_record.is_revoked:
        RefreshToken.objects.filter(family=family).update(is_revoked=True)
        raise ValueError("token_compromised")

    token_record.is_revoked = True
    token_record.save()

    return generate_tokens(family.user, family.device)

def logout_device(device_id: str, user: User):
    RefreshTokenFamily.objects.filter(user=user, device__device_id=device_id).delete()

def logout_all(user: User):
    RefreshTokenFamily.objects.filter(user=user).delete()
