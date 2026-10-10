import logging
from django.db import transaction, IntegrityError
from django.db.models import Q
from django.conf import settings
from rest_framework.exceptions import ValidationError
from wallets.models import Wallet, LedgerAccount, LedgerTransaction, LedgerEntry
from core_api.clock import Clock

logger = logging.getLogger(__name__)

class InsufficientFundsError(ValidationError):
    pass

class IdempotencyConflictError(ValidationError):
    pass

def post_transaction(type: str, entries_data: list, idempotency_key: str, reference_type: str, reference_id: str, created_by: str = 'SYSTEM', metadata: dict = None) -> LedgerTransaction:
    """
    Core double-entry method.
    entries_data = [{'account': account_instance, 'direction': 'DEBIT'/'CREDIT', 'amount_paise': 1000}]
    """
    # 1. Idempotency Check (Fast path)
    existing_txn = LedgerTransaction.objects.filter(idempotency_key=idempotency_key).first()
    if existing_txn:
        # If the payload is radically different, it's a conflict, but for simplicity here we assume
        # the caller provides the same inputs if the key matches.
        if existing_txn.type != type or existing_txn.reference_id != reference_id:
            raise IdempotencyConflictError("Idempotency key collision with different payload")
        return existing_txn

    if not metadata:
        metadata = {}

    total_debit = sum(e['amount_paise'] for e in entries_data if e['direction'] == 'DEBIT')
    total_credit = sum(e['amount_paise'] for e in entries_data if e['direction'] == 'CREDIT')

    if total_debit != total_credit:
        raise ValidationError(f"Transaction does not balance. Debit: {total_debit}, Credit: {total_credit}")

    with transaction.atomic():
        # Try to insert transaction. Unique constraint will fail if another thread won the race
        try:
            txn = LedgerTransaction.objects.create(
                type=type,
                idempotency_key=idempotency_key,
                reference_type=reference_type,
                reference_id=reference_id,
                created_by=created_by,
                metadata=metadata
            )
        except IntegrityError:
            # Another thread just inserted this key
            existing = LedgerTransaction.objects.get(idempotency_key=idempotency_key)
            if existing.type != type:
                raise IdempotencyConflictError("Idempotency key collision")
            return existing

        entries_to_create = []
        for data in entries_data:
            entries_to_create.append(LedgerEntry(
                transaction=txn,
                account=data['account'],
                direction=data['direction'],
                amount_paise=data['amount_paise']
            ))

        LedgerEntry.objects.bulk_create(entries_to_create)
        # Note: The Postgres trigger `ensure_transaction_balance` will fire here to guarantee DB consistency.

        return txn

def get_system_account(type_name: str) -> LedgerAccount:
    """Gets or creates system-level clearing accounts."""
    acc, _ = LedgerAccount.objects.get_or_create(type=type_name, owner_user__isnull=True)
    return acc

def debit_spend(user, amount_paise: int, reason: str, idempotency_key: str, reference_id: str):
    """
    Spends money. Order: Bonus first, then Main.
    Row locks the wallet to prevent race conditions.
    """
    if amount_paise <= 0:
        raise ValidationError("Amount must be positive")

    with transaction.atomic():
        # 1. Lock the wallet!
        try:
            wallet = Wallet.objects.select_for_update().get(user=user)
        except Wallet.DoesNotExist:
            raise ValidationError("Wallet not found")

        # 2. Check idempotent existence (inside the lock to avoid races)
        existing = LedgerTransaction.objects.filter(idempotency_key=idempotency_key).first()
        if existing:
            return existing

        # 3. Check funds
        if (wallet.main_balance + wallet.bonus_balance) < amount_paise:
            raise InsufficientFundsError("INSUFFICIENT_FUNDS")

        # 4. Determine bucket split
        bonus_charge = min(wallet.bonus_balance, amount_paise)
        main_charge = amount_paise - bonus_charge

        # 5. Update wallet (in-memory)
        wallet.bonus_balance -= bonus_charge
        wallet.main_balance -= main_charge
        wallet.version += 1

        # 6. Prepare double-entry payload
        revenue_acc = get_system_account('REVENUE')
        entries = []

        if bonus_charge > 0:
            entries.extend([
                {'account': wallet.bonus_account, 'direction': 'DEBIT', 'amount_paise': bonus_charge},
                {'account': revenue_acc, 'direction': 'CREDIT', 'amount_paise': bonus_charge}
            ])

        if main_charge > 0:
            entries.extend([
                {'account': wallet.main_account, 'direction': 'DEBIT', 'amount_paise': main_charge},
                {'account': revenue_acc, 'direction': 'CREDIT', 'amount_paise': main_charge}
            ])

        # 7. Post transaction (which saves entries and hits triggers)
        txn = post_transaction(
            type='SPEND_MACHINE' if reason == 'MACHINE' else 'SPEND_BOOKING',
            entries_data=entries,
            idempotency_key=idempotency_key,
            reference_type=reason,
            reference_id=reference_id,
            created_by=str(user.id)
        )

        # 8. Save wallet ONLY if transaction posts successfully
        wallet.save()
        return txn

def credit_topup(user, amount_paise: int, source: str, idempotency_key: str, reference_id: str):
    """
    Top-up wallet via gateway or cash kiosk.
    """
    if amount_paise < settings.WALLET_MIN_TOPUP_PAISE or amount_paise > settings.WALLET_MAX_TOPUP_PAISE:
        raise ValidationError(f"Top-up must be between {settings.WALLET_MIN_TOPUP_PAISE} and {settings.WALLET_MAX_TOPUP_PAISE}")

    with transaction.atomic():
        wallet = Wallet.objects.select_for_update().get(user=user)

        existing = LedgerTransaction.objects.filter(idempotency_key=idempotency_key).first()
        if existing:
            return existing

        if wallet.main_balance + amount_paise > settings.WALLET_MAX_BALANCE_PAISE:
            raise ValidationError("Wallet maximum balance exceeded")

        clearing_acc_type = 'GATEWAY_CLEARING' if source == 'GATEWAY' else 'CASH_MACHINE_CLEARING'
        clearing_acc = get_system_account(clearing_acc_type)

        entries = [
            {'account': clearing_acc, 'direction': 'DEBIT', 'amount_paise': amount_paise},
            {'account': wallet.main_account, 'direction': 'CREDIT', 'amount_paise': amount_paise}
        ]

        txn = post_transaction(
            type=f'TOPUP_{source}',
            entries_data=entries,
            idempotency_key=idempotency_key,
            reference_type=source,
            reference_id=reference_id,
            created_by=str(user.id)
        )

        wallet.main_balance += amount_paise
        wallet.version += 1
        wallet.save()

        return txn

def reverse(transaction_id: str, reason: str, admin_id: str):
    """
    Creates a REVERSAL transaction that exactly negates a previous transaction.
    """
    with transaction.atomic():
        orig_txn = LedgerTransaction.objects.prefetch_related('entries__account').get(id=transaction_id)

        if orig_txn.type == 'REVERSAL':
            raise ValidationError("Cannot reverse a reversal")

        reversal_idempotency = f"REV_{orig_txn.id}"
        existing = LedgerTransaction.objects.filter(idempotency_key=reversal_idempotency).first()
        if existing:
            return existing

        # Extract accounts to lock wallets
        account_ids = [e.account_id for e in orig_txn.entries.all()]
        wallets_to_lock = list(Wallet.objects.filter(Q(main_account_id__in=account_ids) | Q(bonus_account_id__in=account_ids)).select_for_update().order_by('id'))

        # Prepare reversing entries (swap debits/credits)
        reversing_entries = []
        for orig_entry in orig_txn.entries.all():
            new_direction = 'CREDIT' if orig_entry.direction == 'DEBIT' else 'DEBIT'
            reversing_entries.append({
                'account': orig_entry.account,
                'direction': new_direction,
                'amount_paise': orig_entry.amount_paise
            })

            # Re-adjust wallet balances in memory
            for w in wallets_to_lock:
                if orig_entry.account_id == w.main_account_id:
                    if new_direction == 'CREDIT': w.main_balance += orig_entry.amount_paise
                    else: w.main_balance -= orig_entry.amount_paise
                elif orig_entry.account_id == w.bonus_account_id:
                    if new_direction == 'CREDIT': w.bonus_balance += orig_entry.amount_paise
                    else: w.bonus_balance -= orig_entry.amount_paise

                # Check invariants
                if w.main_balance < 0 or w.bonus_balance < 0:
                    raise InsufficientFundsError("Reversal would result in negative balance")

        # Post Reversal Transaction
        rev_txn = post_transaction(
            type='REVERSAL',
            entries_data=reversing_entries,
            idempotency_key=reversal_idempotency,
            reference_type='ORIGINAL_TXN',
            reference_id=str(orig_txn.id),
            created_by=admin_id,
            metadata={"reason": reason}
        )

        for w in wallets_to_lock:
            w.version += 1
            w.save()

        return rev_txn
