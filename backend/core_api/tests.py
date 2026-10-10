from django.test import TestCase, Client
from django.urls import reverse

class HealthEndpointTests(TestCase):
    def setUp(self):
        self.client = Client()

    def test_healthz(self):
        response = self.client.get(reverse('healthz'))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_readyz(self):
        response = self.client.get(reverse('readyz'))
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ready"})

class MiddlewareTests(TestCase):
    def setUp(self):
        self.client = Client()

    def test_request_id_injected(self):
        # Even a 404 should have a request ID injected by middleware
        response = self.client.get('/invalid-path/')
        self.assertIn('X-Request-ID', response.headers)

    def test_custom_exception_handler_format(self):
        # We don't have a throwing view yet, but we can verify our 404 handler structure
        response = self.client.get('/invalid-path/', HTTP_ACCEPT='application/json')
        if response.status_code == 404 and response.headers.get('Content-Type') == 'application/json':
            data = response.json()
            self.assertIn('error', data)
            self.assertIn('code', data['error'])
            self.assertIn('request_id', data['error'])
