from django.urls import path
from accounts.views import RequestOtpView, VerifyOtpView, SignupView, LoginView, RefreshTokenView

urlpatterns = [
    path('otp/request', RequestOtpView.as_view(), name='otp-request'),
    path('otp/verify', VerifyOtpView.as_view(), name='otp-verify'),
    path('signup', SignupView.as_view(), name='signup'),
    path('login', LoginView.as_view(), name='login'),
    path('token/refresh', RefreshTokenView.as_view(), name='token-refresh'),
]
