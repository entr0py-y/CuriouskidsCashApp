from django.contrib import admin
from django.urls import path, include
from drf_spectacular.views import SpectacularAPIView, SpectacularSwaggerView
from core_api.views import healthz, readyz

urlpatterns = [
    path('admin/', admin.site.urls),

    # Infrastructure & Health
    path('healthz', healthz, name='healthz'),
    path('readyz', readyz, name='readyz'),

    # OpenAPI Schema & Docs
    path('api/schema/', SpectacularAPIView.as_view(), name='schema'),
    path('api/docs/', SpectacularSwaggerView.as_view(url_name='schema'), name='swagger-ui'),

    # Core User API v1 routing
    path('api/v1/auth/', include('accounts.urls')),
    path('api/v1/wallet/', include('payments.urls')),
    path('api/v1/wallet', include('wallets.urls')),
    path('api/v1/qr/', include('qr.urls')),
    path('api/v1/', include('profiles.urls')),
    path('api/v1/', include('bookings.urls')),
    path('api/v1/', include('offers.urls')),
    path('api/v1/', include('notifications.urls')),

    # Machine Hardware API v1
    path('api/machine/v1/', include('machines.urls')),
]
