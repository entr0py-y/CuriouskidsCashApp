# Skyjumpers Incident Response Runbooks

## Runbook 1: Payment Webhook Failure
1. **Symptoms:** Spike in `webhook.failed` logs or users reporting top-up charged at gateway but not reflected in wallet.
2. **Investigation:** Query `PaymentOrder` records with `status='PENDING'` or `flagged_for_review=True`.
3. **Remediation:**
   - Execute manual Celery reconciliation task: `celery -A config call payments.tasks.reconcile_pending_payment_orders`.
   - Verify Razorpay webhook secret matches `RAZORPAY_WEBHOOK_SECRET` in environment.

---

## Runbook 2: Wallet Balance Mismatch Alert
1. **Symptoms:** Daily Celery reconciliation task triggers `DAILY_RECONCILIATION_ALERT`.
2. **Investigation:** Run `SELECT * FROM wallets_wallet WHERE (main_balance + bonus_balance) != (SELECT SUM(amount) FROM wallets_ledgerentry WHERE wallet_id = wallets_wallet.id)`.
3. **Remediation:**
   - Double-entry trigger guarantees `SUM(DEBIT) == SUM(CREDIT)` per transaction.
   - Investigate flagged `MakerCheckerRequest` or manual admin credits.

---

## Runbook 3: Machine Offline / Silent Alert
1. **Symptoms:** Machine `last_seen` timestamp > 5 minutes.
2. **Investigation:** Check machine network connection, router status, and kiosk hardware power supply.
3. **Remediation:**
   - Verify API credentials in machine `activation_code`.
   - Machine will auto-reconnect and transmit unacknowledged outbox notes upon network restoration.
