import json
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.exceptions import ValidationError
from payments.models import PaymentOrder
from payments.serializers import CreateTopupOrderSerializer, ConfirmCheckoutSerializer, PaymentOrderSerializer
from payments.services import payment_service

class CreateTopupView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = CreateTopupOrderSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = payment_service.create_topup_order(
            user=request.user,
            amount_paise=serializer.validated_data['amount_paise'],
            idempotency_key=serializer.validated_data['idempotency_key']
        )
        return Response(res)

class PaymentOrderDetailView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        order = PaymentOrder.objects.filter(id=pk, user=request.user).first()
        if not order:
            raise ValidationError("Payment order not found.")
        return Response(PaymentOrderSerializer(order).data)

class ConfirmCheckoutView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        serializer = ConfirmCheckoutSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = payment_service.confirm_checkout(
            order_id=str(pk),
            provider_payment_id=serializer.validated_data['provider_payment_id'],
            provider_signature=serializer.validated_data['provider_signature']
        )
        return Response(res)

class RazorpayWebhookView(APIView):
    permission_classes = [AllowAny] # Webhook signature is validated in service

    def post(self, request):
        raw_body = request.body
        signature = request.headers.get('X-Razorpay-Signature', '')

        try:
            payload = json.loads(raw_body.decode('utf-8'))
        except Exception:
            raise ValidationError("Invalid JSON payload.")

        res = payment_service.process_webhook(raw_body, signature, payload)
        return Response(res)
