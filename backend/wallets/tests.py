import threading
import concurrent.futures
from django.db import DatabaseError
from rest_framework.exceptions import ValidationError
from django.urls import reverse
import pytest
from accounts.models import User
from wallets.models import Wallet, LedgerTransaction, LedgerEntry, LedgerAccount
from wallets.services.ledger import credit_topup, debit_spend, post_transaction, reverse as ledger_reverse, InsufficientFundsError
from wallets.tasks import reconcile_wallets

@pytest.mark.django_db(transaction=True)
def test_wallet_initialization():
    # Trigger signal
    user = User.objects.create_user(phone_number='+911111111111', pin='123456')

    wallet = Wallet.objects.get(user=user)
    assert wallet.main_balance == 0
    assert wallet.bonus_balance == 0
    assert wallet.main_account.type == 'USER_WALLET_MAIN'
    assert wallet.bonus_account.type == 'USER_WALLET_BONUS'

@pytest.mark.django_db(transaction=True)
def test_ledger_double_entry_enforcement():
    user = User.objects.create_user(phone_number='+912222222222', pin='123456')
    wallet = Wallet.objects.get(user=user)
    sys_acc = LedgerAccount.objects.create(type='SYSTEM')

    # Test application-level imbalance
    with pytest.raises(ValidationError):
        post_transaction(
            type="TEST",
            entries_data=[
                {'account': wallet.main_account, 'direction': 'CREDIT', 'amount_paise': 100},
                {'account': sys_acc, 'direction': 'DEBIT', 'amount_paise': 50}
            ],
            idempotency_key="TEST_IMBALANCE_1",
            reference_type="TEST",
            reference_id="1"
        )

    # We also have the DB trigger ensuring it.

@pytest.mark.django_db(transaction=True)
def test_concurrency_100_debits_against_10_balance():
    user = User.objects.create_user(phone_number='+913333333333', pin='123456')
    # Credit 10 * 100 paise (10 INR equivalent if 1 INR = 100 paise)
    credit_topup(user, 10000, 'GATEWAY', 'TOPUP_IDEM_1', 'REF_1')

    wallet = Wallet.objects.get(user=user)
    assert wallet.main_balance == 10000

    def attempt_spend(i):
        try:
            debit_spend(user, 1000, 'MACHINE', f'SPEND_IDEM_{i}', f'S_REF_{i}')
            return True
        except InsufficientFundsError:
            return False

    # Execute 100 parallel debit attempts of 100 paise each
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
        results = list(executor.map(attempt_spend, range(100)))

    successes = sum(1 for r in results if r)
    failures = sum(1 for r in results if not r)

    assert successes == 10, f"Expected 10 successes, got {successes}"
    assert failures == 90, f"Expected 90 failures, got {failures}"

    # Assert balance is exactly 0 and never negative
    wallet.refresh_from_db()
    assert wallet.main_balance == 0
    assert wallet.bonus_balance == 0

@pytest.mark.django_db(transaction=True)
def test_concurrency_idempotency_50_parallel():
    user = User.objects.create_user(phone_number='+914444444444', pin='123456')

    def attempt_topup(_):
        credit_topup(user, 5000, 'GATEWAY', 'SAME_IDEM_KEY_123', 'REF_SAME')

    # Execute 50 parallel topups using the EXACT SAME idempotency key
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
        list(executor.map(attempt_topup, range(50)))

    wallet = Wallet.objects.get(user=user)
    # The balance should only be 5000 because 49 requests hit the idempotency lock
    assert wallet.main_balance == 5000
    assert LedgerTransaction.objects.filter(idempotency_key='SAME_IDEM_KEY_123').count() == 1

@pytest.mark.django_db(transaction=True)
def test_reversal_restores_balances():
    user = User.objects.create_user(phone_number='+915555555555', pin='123456')

    txn = credit_topup(user, 8000, 'GATEWAY', 'TOPUP_TO_REVERSE', 'REF_REV')
    wallet = Wallet.objects.get(user=user)
    assert wallet.main_balance == 8000

    # Reverse it
    ledger_reverse(str(txn.id), "Fraudulent transaction", admin_id="ADMIN_1")

    wallet.refresh_from_db()
    assert wallet.main_balance == 0

    # Reversing again should return the idempotent existing reversal, doing nothing
    ledger_reverse(str(txn.id), "Fraudulent transaction", admin_id="ADMIN_1")
    wallet.refresh_from_db()
    assert wallet.main_balance == 0

@pytest.mark.django_db(transaction=True)
def test_reconciliation_job_validates_ledger():
    user = User.objects.create_user(phone_number='+916666666666', pin='123456')
    credit_topup(user, 5000, 'GATEWAY', 'REC_TEST_1', 'REF_1')
    debit_spend(user, 100, 'MACHINE', 'REC_TEST_2', 'REF_2')

    # Run the reconciliation
    result = reconcile_wallets()
    assert result == "SUCCESS"

    # Manually corrupt the wallet balance to simulate a bug/hack
    Wallet.objects.filter(user=user).update(main_balance=999999)

    # Run again, should detect mismatch
    result2 = reconcile_wallets()
    assert "FAILED" in result2
