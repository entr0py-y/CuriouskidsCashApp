import hmac
import hashlib
import uuid
from payments.providers.base import PaymentProvider

class FakePaymentProvider(PaymentProvider):
    def __init__(self, key_id="rzp_test_fake", key_secret="fake_secret_123", webhook_secret="fake_webhook_secret_123"):
        self.key_id = key_id
        self.key_secret = key_secret
        self.webhook_secret = webhook_secret

    def create_order(self, order_id: str, amount_paise: int, currency: str = 'INR') -> dict:
        fake_order_id = f"order_fake_{uuid.uuid4().hex[:12]}"
        return {
            "provider_order_id": fake_order_id,
            "key_id": self.key_id
        }

    def verify_webhook_signature(self, raw_body: bytes, signature: str) -> bool:
        expected = hmac.new(self.webhook_secret.encode(), raw_body, hashlib.sha256).hexdigest()
        return hmac.compare_digest(expected.lower(), (signature or '').lower())

    def verify_checkout_signature(self, provider_order_id: str, provider_payment_id: str, signature: str) -> bool:
        msg = f"{provider_order_id}|{provider_payment_id}"
        expected = hmac.new(self.key_secret.encode(), msg.encode(), hashlib.sha256).hexdigest()
        return hmac.compare_digest(expected.lower(), (signature or '').lower())

    def fetch_order_status(self, provider_order_id: str) -> dict:
        return {
            "provider_order_id": provider_order_id,
            "status": "paid"
        }

    def refund_payment(self, provider_payment_id: str, amount_paise: int, reason: str) -> dict:
        return {
            "provider_refund_id": f"rfnd_fake_{uuid.uuid4().hex[:12]}",
            "status": "processed"
        }
