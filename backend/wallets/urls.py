from django.urls import path
from wallets.views import WalletView, TransactionListView, TransactionDetailView

urlpatterns = [
    path('', WalletView.as_view(), name='wallet-detail'),
    path('transactions', TransactionListView.as_view(), name='transaction-list'),
    path('transactions/<uuid:pk>', TransactionDetailView.as_view(), name='transaction-detail'),
]
