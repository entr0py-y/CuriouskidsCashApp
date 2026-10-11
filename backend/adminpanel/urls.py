from django.urls import path
from adminpanel.views import (
    CustomerSearchView, RevealPiiView, GoodwillCreditView,
    ApproveMakerCheckerView, SuspendUserView, AuditLogListView,
    WalletLiabilityReportView, DailyCollectionsCsvView
)

urlpatterns = [
    path('customers/search', CustomerSearchView.as_view(), name='admin-customer-search'),
    path('customers/<uuid:customer_id>/reveal-pii', RevealPiiView.as_view(), name='admin-reveal-pii'),
    path('customers/<uuid:customer_id>/goodwill-credit', GoodwillCreditView.as_view(), name='admin-goodwill-credit'),
    path('customers/<uuid:customer_id>/suspend', SuspendUserView.as_view(), name='admin-suspend-user'),
    path('maker-checker/<uuid:request_id>/approve', ApproveMakerCheckerView.as_view(), name='admin-maker-checker-approve'),
    path('audit-logs', AuditLogListView.as_view(), name='admin-audit-logs'),
    path('reports/wallet-liability', WalletLiabilityReportView.as_view(), name='admin-wallet-liability'),
    path('reports/daily-collections.csv', DailyCollectionsCsvView.as_view(), name='admin-daily-collections-csv'),
]
