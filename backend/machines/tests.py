import hmac
import hashlib
import json
from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from accounts.models import User
from profiles.models import Branch
from machines.models import Machine, CashSession
from machines.services import machine_service, cash_session_service
from wallets.models import Wallet
from wallets.services.ledger import credit_topup
from qr.services import qr_service
from core_api.clock import Clock

class MachineApiTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(phone_number='+919876543210', pin='123456')
        credit_topup(self.user, 10000, 'GATEWAY', 'TOPUP_TEST_M1', 'REF_1') # 100 INR

        self.branch = Branch.objects.create(
            name="Delhi Branch",
            address="CP",
            latitude=28.6,
            longitude=77.2
        )

        self.kiosk_machine = Machine.objects.create(
            type='VENDING_CASH',
            branch=self.branch,
            name='Cash Kiosk 1',
            status='ONLINE',
            enabled=True,
            activation_code='ACTIVATION_CODE_123'
        )

        self.client = APIClient()

    def _sign_request(self, machine, method: str, path: str, secret: str, payload_dict: dict, nonce: str = "nonce-1", ts_offset: int = 0) -> dict:
        body_bytes = json.dumps(payload_dict, separators=(',', ':')).encode('utf-8') if payload_dict else b''
        body_sha256 = hashlib.sha256(body_bytes).hexdigest()

        ts = str(int(Clock.now().timestamp()) + ts_offset)
        canonical = f"{method.upper()}\n{path}\n{ts}\n{nonce}\n{body_sha256}"
        sig = hmac.new(secret.encode('utf-8'), canonical.encode('utf-8'), hashlib.sha256).hexdigest()

        return {
            'HTTP_X_MACHINE_ID': str(machine.id),
            'HTTP_X_TIMESTAMP': ts,
            'HTTP_X_NONCE': nonce,
            'HTTP_X_SIGNATURE': sig,
        }

    def test_cash_session_start_note_processing_and_close(self):
        _, secret = machine_service.activate_machine('ACTIVATION_CODE_123')
        qr = qr_service.generate_qr_token(self.user, 'dev-kiosk')['token']

        # 1. Start Cash Session
        url_start = reverse('cash-session-start')
        payload_start = {
            'qr_token': qr,
            'idempotency_key': 'SESSION_IDEM_1'
        }
        headers = self._sign_request(self.kiosk_machine, 'POST', url_start, secret, payload_start, nonce='cs-n1')
        resp_start = self.client.post(url_start, data=payload_start, format='json', **headers)
        self.assertEqual(resp_start.status_code, 200)
        session_id = resp_start.json()['session_id']

        # 2. Insert Note 1 (₹100 = 10000 paise)
        url_note = reverse('cash-session-note', kwargs={'session_id': session_id})
        payload_note1 = {
            'note_value': 100,
            'sequence': 1,
            'idempotency_key': f"{self.kiosk_machine.id}:{session_id}:1"
        }
        headers_note1 = self._sign_request(self.kiosk_machine, 'POST', url_note, secret, payload_note1, nonce='cs-n2')
        resp_note1 = self.client.post(url_note, data=payload_note1, format='json', **headers_note1)
        self.assertEqual(resp_note1.status_code, 200)
        self.assertTrue(resp_note1.json()['approved'])
        self.assertEqual(resp_note1.json()['new_balance_paise'], 20000) # 10000 initial + 10000 topup

        # Replay Note 1 (Duplicate retry) -> Idempotent
        headers_note1_replay = self._sign_request(self.kiosk_machine, 'POST', url_note, secret, payload_note1, nonce='cs-n3')
        resp_note1_replay = self.client.post(url_note, data=payload_note1, format='json', **headers_note1_replay)
        self.assertEqual(resp_note1_replay.status_code, 200)
        self.assertEqual(resp_note1_replay.json()['new_balance_paise'], 20000) # Unchanged!

        # 3. Insert Note 2 (₹50 = 5000 paise)
        payload_note2 = {
            'note_value': 50,
            'sequence': 2,
            'idempotency_key': f"{self.kiosk_machine.id}:{session_id}:2"
        }
        headers_note2 = self._sign_request(self.kiosk_machine, 'POST', url_note, secret, payload_note2, nonce='cs-n4')
        resp_note2 = self.client.post(url_note, data=payload_note2, format='json', **headers_note2)
        self.assertEqual(resp_note2.status_code, 200)
        self.assertEqual(resp_note2.json()['new_balance_paise'], 25000) # 20000 + 5000

        # 4. Close Session
        url_close = reverse('cash-session-close', kwargs={'session_id': session_id})
        payload_close = {
            'reason': 'USER_DONE',
            'notes_total': 150 # ₹150
        }
        headers_close = self._sign_request(self.kiosk_machine, 'POST', url_close, secret, payload_close, nonce='cs-n5')
        resp_close = self.client.post(url_close, data=payload_close, format='json', **headers_close)
        self.assertEqual(resp_close.status_code, 200)
        self.assertEqual(resp_close.json()['status'], 'CLOSED')
        self.assertFalse(resp_close.json()['flagged_mismatch'])

    def test_unknown_denomination_rejected_and_flagged(self):
        _, secret = machine_service.activate_machine('ACTIVATION_CODE_123')
        qr = qr_service.generate_qr_token(self.user, 'dev-kiosk')['token']

        # Start Session
        url_start = reverse('cash-session-start')
        payload_start = {'qr_token': qr, 'idempotency_key': 'SESSION_IDEM_2'}
        headers_start = self._sign_request(self.kiosk_machine, 'POST', url_start, secret, payload_start, nonce='cs-n10')
        resp_start = self.client.post(url_start, data=payload_start, format='json', **headers_start)
        session_id = resp_start.json()['session_id']

        # Insert invalid denomination (₹2000 not supported by kiosk / BV)
        url_note = reverse('cash-session-note', kwargs={'session_id': session_id})
        payload_invalid = {
            'note_value': 2000,
            'sequence': 1,
            'idempotency_key': f"{self.kiosk_machine.id}:{session_id}:1"
        }
        headers_invalid = self._sign_request(self.kiosk_machine, 'POST', url_note, secret, payload_invalid, nonce='cs-n11')
        resp_invalid = self.client.post(url_note, data=payload_invalid, format='json', **headers_invalid)
        self.assertEqual(resp_invalid.status_code, 400)

        session = CashSession.objects.get(id=session_id)
        self.assertTrue(session.flagged_for_review)
