from django.test import TestCase, Client
from django.urls import reverse
from django.core.cache import cache
from core_api.clock import Clock
from datetime import timedelta

class AuthTests(TestCase):
    def setUp(self):
        cache.clear()
        self.client = Client()
        self.phone = "+919876543210"
        self.device_data = {"id": "dev-123", "name": "Pixel"}

    def test_otp_flow_and_signup(self):
        # Request OTP
        resp1 = self.client.post(reverse('otp-request'), {'phone': self.phone, 'purpose': 'SIGNUP'}, content_type='application/json')
        self.assertEqual(resp1.status_code, 200)

        # Bypass the fake to grab the hash directly from cache to test verification
        cached_otp = cache.get(f"otp_data_{self.phone}_SIGNUP")
        self.assertIsNotNone(cached_otp)

        # We can't easily guess the OTP because it's hashed and random.
        # But we can test invalid OTP
        resp2 = self.client.post(reverse('otp-verify'), {'phone': self.phone, 'otp': '000000', 'purpose': 'SIGNUP'}, content_type='application/json')
        self.assertEqual(resp2.status_code, 400)
        self.assertEqual(resp2.json()['error']['code'], 'INVALID')
        self.assertIn("Invalid OTP", resp2.json()['error']['message'])

    def test_pin_validation(self):
        from accounts.services.pin import is_weak_pin
        self.assertTrue(is_weak_pin('111111', self.phone))
        self.assertTrue(is_weak_pin('123456', self.phone))
        self.assertTrue(is_weak_pin('543210', self.phone))
        self.assertTrue(is_weak_pin('543210', self.phone))
        self.assertTrue(is_weak_pin('654321', self.phone))

        # Phone suffix check
        phone = "+919998881234"
        self.assertTrue(is_weak_pin('881234', phone))

        # Strong PIN
        self.assertFalse(is_weak_pin('835192', phone))

    def test_lockout_progression(self):
        from accounts.services.auth import record_failed_login, check_lockout

        # 4 fails -> no lock
        for _ in range(4):
            record_failed_login(self.phone, "127.0.0.1")

        # Should not raise
        check_lockout(self.phone)

        # 5th fail -> locks for 15m
        record_failed_login(self.phone, "127.0.0.1")
        with self.assertRaises(ValueError) as e:
            check_lockout(self.phone)
        self.assertEqual(str(e.exception), "account_locked")

        # Fast forward time to test unlock
        Clock.freeze(Clock.now() + timedelta(minutes=16))
        # Should not raise now because time passed
        check_lockout(self.phone)
        Clock.unfreeze()
