import json
import hmac
import hashlib
from django.test import TestCase, override_settings
from django.urls import reverse
from rest_framework.test import APIClient
from rest_framework.exceptions import ValidationError
from accounts.models import User
from payments.models import PaymentOrder, PaymentRefund
from payments.providers.fake_provider import FakePaymentProvider
from payments.services import payment_service, refund_service
from wallets.models import Wallet

@override_settings(TESTING=True, USE_FAKE_PAYMENT_PROVIDER=True)
class PaymentGatewayTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(phone_number='+919876543210', pin='123456')
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)
        self.provider = FakePaymentProvider()

    def test_create_topup_order_limits_and_idempotency(self):
        url = reverse('topup-create')

        # Below min limit (e.g. 10 paise) -> Should fail
        resp = self.client.post(url, {'amount_paise': 10, 'idempotency_key': 'KEY_1'}, format='json')
        self.assertEqual(resp.status_code, 400)

        # Valid topup
        resp_valid = self.client.post(url, {'amount_paise': 5000, 'idempotency_key': 'KEY_1'}, format='json')
        self.assertEqual(resp_valid.status_code, 200)
        data = resp_valid.json()
        self.assertEqual(data['amount_paise'], 5000)
        self.assertIn('provider_order_id', data)

        # Duplicate idempotency key -> Returns existing order
        resp_dup = self.client.post(url, {'amount_paise': 5000, 'idempotency_key': 'KEY_1'}, format='json')
        self.assertEqual(resp_dup.json()['id'], data['id'])

    def test_webhook_invalid_signature_rejected(self):
        url = reverse('topup-webhook')
        payload = {"event": "payment.captured", "payload": {}}
        raw_bytes = json.dumps(payload).encode('utf-8')

        headers = {'HTTP_X_RAZORPAY_SIGNATURE': 'invalid_sig'}
        resp = self.client.post(url, data=raw_bytes, content_type='application/json', **headers)
        self.assertEqual(resp.status_code, 400)

    def test_webhook_captured_credits_wallet_in_ledger(self):
        # 1. Create order
        order_res = payment_service.create_topup_order(self.user, 5000, 'IDEM_WEBHOOK_1')
        p_order_id = order_res['provider_order_id']

        # 2. Build webhook payload
        payload = {
            "event": "payment.captured",
            "payload": {
                "payment": {
                    "entity": {
                        "id": "pay_fake_999",
                        "order_id": p_order_id,
                        "amount": 5000,
                        "currency": "INR"
                    }
                }
            }
        }
        raw_body = json.dumps(payload, separators=(',', ':')).encode('utf-8')
        sig = hmac.new(self.provider.webhook_secret.encode(), raw_body, hashlib.sha256).hexdigest()

        url = reverse('topup-webhook')
        headers = {'HTTP_X_RAZORPAY_SIGNATURE': sig}
        resp = self.client.post(url, data=raw_body, content_type='application/json', **headers)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()['status'], 'PAID')

        # Verify wallet credited
        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 5000)

        # 3. Duplicate Webhook Retry -> Does not double credit
        resp_dup = self.client.post(url, data=raw_body, content_type='application/json', **headers)
        self.assertEqual(resp_dup.status_code, 200)
        self.assertEqual(resp_dup.json()['status'], 'ALREADY_PROCESSED')

        wallet.refresh_from_db()
        self.assertEqual(wallet.main_balance, 5000) # Unchanged

    def test_webhook_amount_mismatch_flags_order(self):
        order_res = payment_service.create_topup_order(self.user, 5000, 'IDEM_WEBHOOK_MISMATCH')
        p_order_id = order_res['provider_order_id']

        payload = {
            "event": "payment.captured",
            "payload": {
                "payment": {
                    "entity": {
                        "id": "pay_fake_mismatch",
                        "order_id": p_order_id,
                        "amount": 1000, # Expected 5000!
                        "currency": "INR"
                    }
                }
            }
        }
        raw_body = json.dumps(payload, separators=(',', ':')).encode('utf-8')
        sig = hmac.new(self.provider.webhook_secret.encode(), raw_body, hashlib.sha256).hexdigest()

        url = reverse('topup-webhook')
        headers = {'HTTP_X_RAZORPAY_SIGNATURE': sig}
        resp = self.client.post(url, data=raw_body, content_type='application/json', **headers)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()['status'], 'FLAGGED')

        order = PaymentOrder.objects.get(id=order_res['id'])
        self.assertTrue(order.flagged_for_review)

        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 0) # Not credited

    def test_confirm_checkout_verifies_signature(self):
        order_res = payment_service.create_topup_order(self.user, 5000, 'IDEM_CONFIRM')
        p_order_id = order_res['provider_order_id']
        p_pay_id = "pay_fake_confirm_123"

        msg = f"{p_order_id}|{p_pay_id}"
        sig = hmac.new(self.provider.key_secret.encode(), msg.encode(), hashlib.sha256).hexdigest()

        url = reverse('topup-confirm', kwargs={'pk': order_res['id']})
        resp = self.client.post(url, {'provider_payment_id': p_pay_id, 'provider_signature': sig}, format='json')
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(resp.json()['confirmed'])

    def test_admin_refund_topup_debits_wallet(self):
        # Top-up first
        order_res = payment_service.create_topup_order(self.user, 5000, 'IDEM_REFUND_TEST')
        p_order_id = order_res['provider_order_id']

        payload = {
            "event": "payment.captured",
            "payload": {"payment": {"entity": {"id": "pay_fake_rfnd", "order_id": p_order_id, "amount": 5000, "currency": "INR"}}}
        }
        raw_body = json.dumps(payload, separators=(',', ':')).encode('utf-8')
        sig = hmac.new(self.provider.webhook_secret.encode(), raw_body, hashlib.sha256).hexdigest()
        self.client.post(reverse('topup-webhook'), data=raw_body, content_type='application/json', HTTP_X_RAZORPAY_SIGNATURE=sig)

        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 5000)

        # Refund topup
        refund = refund_service.admin_refund_topup(order_res['id'], "User requested refund", "ADMIN_1")
        self.assertIsNotNone(refund.provider_refund_id)

        wallet.refresh_from_db()
        self.assertEqual(wallet.main_balance, 0)
