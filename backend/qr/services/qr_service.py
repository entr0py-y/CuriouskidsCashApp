import jwt
import uuid
import datetime
from django.core.cache import cache
from rest_framework.exceptions import ValidationError, Throttled
from qr.crypto import get_private_pem, get_public_pem, KID
from core_api.clock import Clock

QR_PREFIX = "SKYJ1:"
TOKEN_TTL_SECONDS = 45

def generate_qr_token(user, device_id: str) -> dict:
    """
    Generates a signed, short-lived (45s), device-bound QR token for the user.
    """
    if not user.is_active or user.deleted_at is not None:
        raise ValidationError("User account is suspended or deleted.")

    # Rate limiting issuance: max 5 tokens per 10 seconds per device
    rate_key = f"qr_rate_{user.id}_{device_id}"
    count = cache.get(rate_key, 0)
    if count >= 5:
        raise Throttled(detail="Too many QR token requests. Please wait.")
    cache.set(rate_key, count + 1, timeout=10)

    now = Clock.now()
    expires_at = now + datetime.timedelta(seconds=TOKEN_TTL_SECONDS)
    jti = str(uuid.uuid4())

    payload = {
        "user_ref": str(user.id),
        "jti": jti,
        "iat": int(now.timestamp()),
        "exp": int(expires_at.timestamp()),
        "device_id": device_id,
        "scope": "wallet",
        "ver": "1"
    }

    private_pem = get_private_pem()
    raw_jwt = jwt.encode(payload, private_pem, algorithm="ES256", headers={"kid": KID})
    formatted_qr = f"{QR_PREFIX}{raw_jwt}"

    return {
        "token": formatted_qr,
        "expires_at": expires_at.isoformat()
    }

def decode_and_verify_qr_token(qr_text: str) -> dict:
    """
    Verifies signature, format prefix, and expiration of the QR token.
    Returns decoded payload dictionary.
    """
    if not qr_text.startswith(QR_PREFIX):
        raise ValidationError("Invalid QR code prefix.")

    raw_jwt = qr_text[len(QR_PREFIX):]

    try:
        public_pem = get_public_pem()
        payload = jwt.decode(raw_jwt, public_pem, algorithms=["ES256"])
    except jwt.ExpiredSignatureError:
        raise ValidationError("TOKEN_EXPIRED")
    except jwt.InvalidTokenError:
        raise ValidationError("TOKEN_INVALID")

    # Check clock expiration against mockable Clock
    if payload.get("exp") < int(Clock.now().timestamp()):
        raise ValidationError("TOKEN_EXPIRED")

    return payload

def consume_jti_for_spending(jti: str):
    """
    Ensures single-use for spending by setting jti in Redis.
    If jti already exists in Redis, raises TOKEN_REUSED.
    """
    redis_key = f"qr_consumed_jti_{jti}"
    # setnx equivalent using Django cache
    is_new = cache.add(redis_key, "1", timeout=120)
    if not is_new:
        raise ValidationError("TOKEN_REUSED")
