from rest_framework import serializers
from wallets.models import Wallet, LedgerTransaction

class WalletSerializer(serializers.ModelSerializer):
    total_paise = serializers.SerializerMethodField()
    main_paise = serializers.IntegerField(source='main_balance')
    bonus_paise = serializers.IntegerField(source='bonus_balance')

    class Meta:
        model = Wallet
        fields = ['main_paise', 'bonus_paise', 'total_paise', 'updated_at']

    def get_total_paise(self, obj):
        return obj.main_balance + obj.bonus_balance

class LedgerTransactionSerializer(serializers.ModelSerializer):
    amount = serializers.SerializerMethodField()
    direction = serializers.SerializerMethodField()
    title = serializers.SerializerMethodField()
    balance_after = serializers.SerializerMethodField()
    timestamp = serializers.DateTimeField(source='created_at')

    class Meta:
        model = LedgerTransaction
        fields = ['id', 'title', 'amount', 'direction', 'balance_after', 'timestamp', 'status', 'reference_type']

    def get_amount(self, obj):
        # Determine the user's specific entries to calculate net change
        user = self.context['request'].user
        user_entries = obj.entries.filter(account__owner_user=user)
        return sum(e.amount_paise for e in user_entries if e.direction == 'CREDIT') - sum(e.amount_paise for e in user_entries if e.direction == 'DEBIT')

    def get_direction(self, obj):
        amount = self.get_amount(obj)
        if amount > 0: return 'CREDIT'
        if amount < 0: return 'DEBIT'
        return 'NEUTRAL'

    def get_amount(self, obj):
        return abs(self._get_net_amount(obj))

    def _get_net_amount(self, obj):
        user = self.context['request'].user
        user_entries = obj.entries.filter(account__owner_user=user)
        return sum(e.amount_paise for e in user_entries if e.direction == 'CREDIT') - sum(e.amount_paise for e in user_entries if e.direction == 'DEBIT')

    def get_title(self, obj):
        # Map raw internal types to human readable strings
        types = {
            'TOPUP_GATEWAY': 'Online Top-up',
            'TOPUP_CASH_MACHINE': 'Cash Kiosk Top-up',
            'SPEND_MACHINE': 'Game Play',
            'SPEND_BOOKING': 'Ticket Purchase',
            'BONUS_GRANT': 'Bonus Received',
            'REVERSAL': 'Refund / Reversal'
        }
        return types.get(obj.type, obj.type)

    def get_balance_after(self, obj):
        # We'd ideally pre-calculate this or derive it during the reconciliation job and store it
        # For simplicity in B4 MVP, returning None if we haven't materialized it yet.
        return None
