from django.urls import path
from qr.views import QrTokenView, QrKeysView

urlpatterns = [
    path('token', QrTokenView.as_view(), name='qr-token'),
    path('keys', QrKeysView.as_view(), name='qr-keys'),
]
