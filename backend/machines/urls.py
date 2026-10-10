from django.urls import path
from machines.views import ActivateMachineView, IdentifyView, ChargeView, RefundView, HeartbeatView

urlpatterns = [
    path('activate', ActivateMachineView.as_view(), name='machine-activate'),
    path('identify', IdentifyView.as_view(), name='machine-identify'),
    path('charge', ChargeView.as_view(), name='machine-charge'),
    path('charge/<uuid:transaction_id>/refund', RefundView.as_view(), name='machine-refund'),
    path('heartbeat', HeartbeatView.as_view(), name='machine-heartbeat'),
]
