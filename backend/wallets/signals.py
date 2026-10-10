from django.dispatch import receiver
from django.db import transaction
from accounts.signals import user_created
from wallets.models import Wallet, LedgerAccount

@receiver(user_created)
def create_wallet_for_new_user(sender, user, **kwargs):
    """
    Creates the wallet and its associated ledger accounts when a new user signs up.
    Guarded against duplicate creation attempts.
    """
    if not Wallet.objects.filter(user=user).exists():
        with transaction.atomic():
            main_acc = LedgerAccount.objects.create(type='USER_WALLET_MAIN', owner_user=user)
            bonus_acc = LedgerAccount.objects.create(type='USER_WALLET_BONUS', owner_user=user)
            Wallet.objects.create(user=user, main_account=main_acc, bonus_account=bonus_acc)
