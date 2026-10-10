from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from accounts.models import User
from qr.services import qr_service
from core_api.clock import Clock
from datetime import timedelta
import jwt

class QrTokenTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(phone_number='+919876543210', pin='123456')
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)

    def test_qr_token_generation_format(self):
        res = qr_service.generate_qr_token(self.user, device_id="dev-123")
        self.assertTrue(res['token'].startswith("SKYJ1:"))

        # Verify decoding
        payload = qr_service.decode_and_verify_qr_token(res['token'])
        self.assertEqual(payload['user_ref'], str(self.user.id))
        self.assertEqual(payload['device_id'], "dev-123")
        self.assertEqual(payload['scope'], "wallet")

    def test_qr_token_expiration(self):
        res = qr_service.generate_qr_token(self.user, device_id="dev-123")

        # Advance clock by 50 seconds (expiry is 45s)
        Clock.freeze(Clock.now() + timedelta(seconds=50))

        with self.assertRaises(Exception):
            qr_service.decode_and_verify_qr_token(res['token'])

        Clock.unfreeze()

    def test_single_use_jti_consumption(self):
        res = qr_service.generate_qr_token(self.user, device_id="dev-123")
        payload = qr_service.decode_and_verify_qr_token(res['token'])

        jti = payload['jti']

        # First consumption -> Succeeded
        qr_service.consume_jti_for_spending(jti)

        # Second consumption -> Fails with TOKEN_REUSED
        with self.assertRaises(Exception) as ctx:
            qr_service.consume_jti_for_spending(jti)

        self.assertIn("TOKEN_REUSED", str(ctx.exception))
