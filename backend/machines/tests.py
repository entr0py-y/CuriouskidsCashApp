import hmac
import hashlib
import json
from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from accounts.models import User
from profiles.models import Branch
from machines.models import Machine
from machines.services import machine_service
from wallets.services.ledger import credit_topup
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

        self.machine = Machine.objects.create(
            type='GAME',
            branch=self.branch,
            name='Tekken Cabinet',
            status='ONLINE',
            enabled=True,
            activation_code='ACTIVATION_CODE_123',
            price_list={"TEKKEN": 5000} # 50 INR
        )

        self.client = APIClient()

    def _sign_request(self, method: str, path: str, secret: str, payload_dict: dict, nonce: str = "nonce-1", ts_offset: int = 0) -> dict:
        # Match DRF JSONRenderer formatting (no whitespace after separators)
        body_bytes = json.dumps(payload_dict, separators=(',', ':')).encode('utf-8') if payload_dict else b''
        body_sha256 = hashlib.sha256(body_bytes).hexdigest()

        ts = str(int(Clock.now().timestamp()) + ts_offset)
        canonical = f"{method.upper()}\n{path}\n{ts}\n{nonce}\n{body_sha256}"
        sig = hmac.new(secret.encode('utf-8'), canonical.encode('utf-8'), hashlib.sha256).hexdigest()

        return {
            'HTTP_X_MACHINE_ID': str(self.machine.id),
            'HTTP_X_TIMESTAMP': ts,
            'HTTP_X_NONCE': nonce,
            'HTTP_X_SIGNATURE': sig,
        }

    def test_one_time_machine_activation(self):
        resp = self.client.post(
            reverse('machine-activate'),
            {'activation_code': 'ACTIVATION_CODE_123'},
            format='json'
        )
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data['machine_id'], str(self.machine.id))
        self.assertIn('secret', data)

        # Activation code is now consumed
        self.machine.refresh_from_db()
        self.assertIsNone(self.machine.activation_code)

    def test_hmac_auth_and_clock_skew(self):
        # First activate to get secret
        _, secret = machine_service.activate_machine('ACTIVATION_CODE_123')

        url = reverse('machine-heartbeat')
        payload = {'status': 'ONLINE'}

        # 1. Valid Signature
        headers = self._sign_request('POST', url, secret, payload, nonce='n1')
        resp = self.client.post(url, data=payload, format='json', **headers)
        self.assertEqual(resp.status_code, 200)

        # 2. Clock skew > 60s -> Rejected
        headers_skew = self._sign_request('POST', url, secret, payload, nonce='n2', ts_offset=120)
        resp_skew = self.client.post(url, data=payload, format='json', **headers_skew)
        self.assertEqual(resp_skew.status_code, 403)

        # 3. Nonce reuse -> Rejected
        headers_reuse = self._sign_request('POST', url, secret, payload, nonce='n1')
        resp_reuse = self.client.post(url, data=payload, format='json', **headers_reuse)
        self.assertEqual(resp_reuse.status_code, 403)

    def test_charge_game_server_price_enforcement(self):
        _, secret = machine_service.activate_machine('ACTIVATION_CODE_123')
        from qr.services import qr_service
        qr = qr_service.generate_qr_token(self.user, 'dev-1')['token']

        url = reverse('machine-charge')
        # Attacker tries to pass fake price in request body
        payload = {
            'qr_token': qr,
            'game_code': 'TEKKEN',
            'idempotency_key': 'CHARGE_IDEM_1',
            'price_paise': 10 # Attempted price tampering!
        }

        headers = self._sign_request('POST', url, secret, payload, nonce='charge-n1')
        resp = self.client.post(url, data=payload, format='json', **headers)
        self.assertEqual(resp.status_code, 200)

        # Verify server charged 5000 paise (50 INR) from price_list, ignoring the 10 in body
        data = resp.json()
        self.assertTrue(data['approved'])
        self.assertEqual(data['charged_paise'], 5000)
        self.assertEqual(data['balance_after_paise'], 5000)
