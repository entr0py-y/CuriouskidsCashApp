from django.urls import path
from payments.views import CreateTopupView, PaymentOrderDetailView, ConfirmCheckoutView, RazorpayWebhookView

urlpatterns = [
    path('topups', CreateTopupView.as_view(), name='topup-create'),
    path('topups/webhook', RazorpayWebhookView.as_view(), name='topup-webhook'),
    path('topups/<uuid:pk>', PaymentOrderDetailView.as_view(), name='topup-detail'),
    path('topups/<uuid:pk>/confirm', ConfirmCheckoutView.as_view(), name='topup-confirm'),
]
