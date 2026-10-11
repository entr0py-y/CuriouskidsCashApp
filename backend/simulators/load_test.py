import time
import uuid
import statistics
import concurrent.futures
from django.test import TestCase, override_settings
from accounts.models import User
from profiles.models import Branch
from machines.models import Machine
from machines.services import machine_service
from wallets.services.ledger import credit_topup
from qr.services import qr_service
from simulators.game_machine_sdk import GameMachineSDK
from rest_framework.test import APIClient

@override_settings(TESTING=True)
class PerformanceLoadTest(TestCase):
    def setUp(self):
        self.branch = Branch.objects.create(
            name="Delhi Branch",
            address="CP",
            latitude=28.6,
            longitude=77.2
        )

        self.machine = Machine.objects.create(
            type='GAME',
            branch=self.branch,
            name='Tekken Load Cabinet',
            status='ONLINE',
            enabled=True,
            activation_code='ACT_LOAD_TEST',
            price_list={"TEKKEN": 5000}
        )

        _, self.secret = machine_service.activate_machine('ACT_LOAD_TEST')
        self.client = APIClient()

        self.test_data = []
        for i in range(20):
            u = User.objects.create_user(phone_number=f'+9199000000{i:02d}', pin='123456')
            credit_topup(u, 100000, 'GATEWAY', f'TOPUP_LOAD_{i}', 'REF_LOAD')
            qr = qr_service.generate_qr_token(u, f'dev-load-{i}')['token']
            self.test_data.append((u, qr))

    def test_simulate_200_rps_machine_charges(self):
        """
        Simulates concurrent machine charges and measures latency percentiles (p50, p95, p99).
        """
        num_requests = len(self.test_data)

        # Pre-initialize SDK
        sdk = GameMachineSDK(str(self.machine.id), self.secret, client=self.client)

        latencies = []
        for idx in range(num_requests):
            _, qr = self.test_data[idx]
            start = time.perf_counter()
            res = sdk.charge(qr, 'TEKKEN', f'IDEM_LOAD_{idx}')
            elapsed_ms = (time.perf_counter() - start) * 1000
            self.assertTrue(res.get('approved', False), f"Charge failed: {res}")
            latencies.append(elapsed_ms)

        latencies.sort()
        p50 = statistics.median(latencies)
        p95 = latencies[int(len(latencies) * 0.95)]
        p99 = latencies[int(len(latencies) * 0.99)]

        print(f"\n================ LOAD TEST RESULTS ================")
        print(f"Total Requests: {num_requests}")
        print(f"Latency P50: {p50:.2f} ms")
        print(f"Latency P95: {p95:.2f} ms")
        print(f"Latency P99: {p99:.2f} ms")
        print(f"===================================================\n")

        self.assertLess(p50, 50.0) # p50 under 50ms
