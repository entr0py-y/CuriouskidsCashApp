import logging
from celery import shared_task
from django.db import transaction
from django.db.models import Sum
from wallets.models import Wallet, LedgerEntry, LedgerAccount

logger = logging.getLogger(__name__)

@shared_task
def reconcile_wallets():
    """
    Background job to recompute wallet balances from raw entries
    and alert if there is a mismatch.
    """
    mismatches = []

    with transaction.atomic():
        # Using iterator() to prevent memory explosion if there are many users
        for wallet in Wallet.objects.all().iterator():

            # Recompute Main Account
            main_credits = LedgerEntry.objects.filter(
                account=wallet.main_account, direction='CREDIT'
            ).aggregate(total=Sum('amount_paise'))['total'] or 0

            main_debits = LedgerEntry.objects.filter(
                account=wallet.main_account, direction='DEBIT'
            ).aggregate(total=Sum('amount_paise'))['total'] or 0

            computed_main = main_credits - main_debits

            # Recompute Bonus Account
            bonus_credits = LedgerEntry.objects.filter(
                account=wallet.bonus_account, direction='CREDIT'
            ).aggregate(total=Sum('amount_paise'))['total'] or 0

            bonus_debits = LedgerEntry.objects.filter(
                account=wallet.bonus_account, direction='DEBIT'
            ).aggregate(total=Sum('amount_paise'))['total'] or 0

            computed_bonus = bonus_credits - bonus_debits

            if wallet.main_balance != computed_main or wallet.bonus_balance != computed_bonus:
                mismatches.append(wallet.user_id)
                logger.error(
                    f"LEDGER MISMATCH DETECTED FOR USER {wallet.user_id}! "
                    f"Main DB: {wallet.main_balance}, Computed: {computed_main}. "
                    f"Bonus DB: {wallet.bonus_balance}, Computed: {computed_bonus}."
                )

    if not mismatches:
        logger.info("Reconciliation complete. All wallets balance perfectly.")
        return "SUCCESS"
    else:
        return f"FAILED - {len(mismatches)} mismatches detected."
