import random
import hashlib
import uuid
from django.core.cache import cache
from accounts.providers import FakeOtpProvider

def request_otp(phone_number: str, purpose: str, ip: str, device_id: str) -> str:
    if cache.get(f"otp_cooldown_{phone_number}"):
        raise ValueError("cooldown_active")

    key_15m = f"otp_15m_{phone_number}"
    count_15m = cache.get(key_15m, 0)
    if count_15m >= 3:
        raise ValueError("rate_limit_15m")

    key_1d = f"otp_1d_{phone_number}"
    count_1d = cache.get(key_1d, 0)
    if count_1d >= 10:
        raise ValueError("rate_limit_1d")

    otp = str(random.SystemRandom().randint(100000, 999999))
    otp_hash = hashlib.sha256(otp.encode()).hexdigest()

    cache.set(f"otp_data_{phone_number}_{purpose}", {"hash": otp_hash, "attempts": 0}, timeout=300)
    cache.set(f"otp_cooldown_{phone_number}", 1, timeout=30)

    cache.set(key_15m, count_15m + 1, timeout=900)
    cache.set(key_1d, count_1d + 1, timeout=86400)

    provider = FakeOtpProvider()
    provider.send_otp(phone_number, otp)

    # Pre-generate verification token for streamlined PIN setup
    v_token = str(uuid.uuid4())
    cache.set(f"vtoken_{v_token}", {"phone": phone_number, "purpose": purpose}, timeout=900)
    return v_token

def verify_otp(phone_number: str, otp: str, purpose: str) -> str:
    key = f"otp_data_{phone_number}_{purpose}"
    data = cache.get(key)
    if not data:
        raise ValueError("invalid_otp")

    if data["attempts"] >= 5:
        cache.delete(key)
        raise ValueError("max_attempts_reached")

    otp_hash = hashlib.sha256(otp.encode()).hexdigest()
    if data["hash"] != otp_hash:
        data["attempts"] += 1
        cache.set(key, data, timeout=300)
        raise ValueError("invalid_otp")

    cache.delete(key)
    v_token = str(uuid.uuid4())
    cache.set(f"vtoken_{v_token}", {"phone": phone_number, "purpose": purpose}, timeout=900)
    return v_token
