from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.generics import ListAPIView, RetrieveAPIView
from wallets.models import Wallet, LedgerTransaction
from wallets.serializers import WalletSerializer, LedgerTransactionSerializer

class WalletView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        wallet = Wallet.objects.get(user=request.user)
        serializer = WalletSerializer(wallet)
        return Response(serializer.data)

class TransactionListView(ListAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = LedgerTransactionSerializer

    def get_queryset(self):
        # Get all transactions that have an entry affecting an account owned by the user
        return LedgerTransaction.objects.filter(
            entries__account__owner_user=self.request.user
        ).distinct().order_by('-created_at')

class TransactionDetailView(RetrieveAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = LedgerTransactionSerializer

    def get_queryset(self):
        return LedgerTransaction.objects.filter(
            entries__account__owner_user=self.request.user
        ).distinct()
