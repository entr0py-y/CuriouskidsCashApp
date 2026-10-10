import hmac
import hashlib
from django.core.cache import cache
from rest_framework import authentication
from rest_framework.exceptions import AuthenticationFailed
from machines.models import Machine
from core_api.clock import Clock

class MachineHMACAuthentication(authentication.BaseAuthentication):
    """
    HMAC-SHA256 Request Signing Authentication for Machines.
    Headers required:
    - X-Machine-ID: UUID
    - X-Timestamp: Unix Epoch Seconds
    - X-Nonce: Random string / UUID
    - X-Signature: Hex digest of HMAC-SHA256(secret, canonical_string)

    Canonical String: METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256
    """

    def authenticate(self, request):
        machine_id = request.headers.get('X-Machine-ID')
        timestamp_str = request.headers.get('X-Timestamp')
        nonce = request.headers.get('X-Nonce')
        signature = request.headers.get('X-Signature')

        if not (machine_id and timestamp_str and nonce and signature):
            return None # Fallback or unauthenticated

        try:
            machine = Machine.objects.get(id=machine_id)
        except Machine.DoesNotExist:
            raise AuthenticationFailed('MACHINE_NOT_FOUND')

        if not machine.enabled:
            raise AuthenticationFailed('MACHINE_DISABLED')

        # 1. Clock skew check (max 60 seconds)
        try:
            ts = int(timestamp_str)
        except ValueError:
            raise AuthenticationFailed('INVALID_TIMESTAMP')

        current_ts = int(Clock.now().timestamp())
        if abs(current_ts - ts) > 60:
            raise AuthenticationFailed('CLOCK_SKEW_REJECTED')

        # 2. Nonce reuse check (TTL 120 seconds in Redis)
        nonce_key = f"machine_nonce_{machine_id}_{nonce}"
        if not cache.add(nonce_key, "1", timeout=120):
            raise AuthenticationFailed('NONCE_REUSED')

        # 3. Canonical String Construction & Signature Check
        body_bytes = request.body or b''
        body_sha256 = hashlib.sha256(body_bytes).hexdigest()

        canonical = f"{request.method.upper()}\n{request.path}\n{timestamp_str}\n{nonce}\n{body_sha256}"
        expected_sig = hmac.new(
            machine.secret.encode('utf-8'),
            canonical.encode('utf-8'),
            hashlib.sha256
        ).hexdigest()

        if not hmac.compare_digest(expected_sig.lower(), signature.lower()):
            raise AuthenticationFailed('INVALID_SIGNATURE')

        request.machine = machine
        return (machine, None)
