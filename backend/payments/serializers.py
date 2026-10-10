from rest_framework import serializers
from payments.models import PaymentOrder

class CreateTopupOrderSerializer(serializers.Serializer):
    amount_paise = serializers.IntegerField(min_value=100)
    idempotency_key = serializers.CharField(max_length=255)

class ConfirmCheckoutSerializer(serializers.Serializer):
    provider_payment_id = serializers.CharField(max_length=255)
    provider_signature = serializers.CharField(max_length=255)

class PaymentOrderSerializer(serializers.ModelSerializer):
    class Meta:
        model = PaymentOrder
        fields = [
            'id', 'amount_paise', 'currency', 'status', 'idempotency_key',
            'provider', 'provider_order_id', 'provider_payment_id', 'created_at'
        ]
