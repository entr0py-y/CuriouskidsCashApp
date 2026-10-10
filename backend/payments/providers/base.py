from abc import ABC, abstractmethod

class PaymentProvider(ABC):
    @abstractmethod
    def create_order(self, order_id: str, amount_paise: int, currency: str) -> dict:
        """Creates an order at the payment gateway."""
        pass

    @abstractmethod
    def verify_webhook_signature(self, raw_body: bytes, signature: str) -> bool:
        """Verifies the webhook signature over the raw body using the webhook secret."""
        pass

    @abstractmethod
    def verify_checkout_signature(self, provider_order_id: str, provider_payment_id: str, signature: str) -> bool:
        """Verifies the checkout signature returned to the mobile app."""
        pass

    @abstractmethod
    def fetch_order_status(self, provider_order_id: str) -> dict:
        """Queries the provider API for true status of an order."""
        pass

    @abstractmethod
    def refund_payment(self, provider_payment_id: str, amount_paise: int, reason: str) -> dict:
        """Triggers a refund at the payment provider."""
        pass
