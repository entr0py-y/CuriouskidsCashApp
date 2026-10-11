from django.urls import path
from notifications.views import RegisterDeviceTokenView

urlpatterns = [
    path('notifications/device-token', RegisterDeviceTokenView.as_view(), name='device-token-register'),
]
