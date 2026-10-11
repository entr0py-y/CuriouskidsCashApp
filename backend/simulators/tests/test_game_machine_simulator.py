import concurrent.futures
from django.test import TestCase
from rest_framework.test import APIClient
from accounts.models import User
from profiles.models import Branch
from machines.models import Machine
from machines.services import machine_service
from wallets.models import Wallet
from wallets.services.ledger import credit_topup
from qr.services import qr_service
from simulators.game_machine_sdk import GameMachineSDK

class GameMachineSimulatorTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(phone_number='+919876543210', pin='123456')
        credit_topup(self.user, 10000, 'GATEWAY', 'TOPUP_SIM_1', 'REF_SIM') # 100 INR

        self.branch = Branch.objects.create(
            name="Delhi Branch",
            address="CP",
            latitude=28.6,
            longitude=77.2
        )

        self.machine = Machine.objects.create(
            type='GAME',
            branch=self.branch,
            name='Tekken Arcade Cabinet 1',
            status='ONLINE',
            enabled=True,
            activation_code='ACT_CODE_SIM_1',
            price_list={"TEKKEN": 5000} # 50 INR
        )

        _, self.secret = machine_service.activate_machine('ACT_CODE_SIM_1')
        self.client = APIClient()

    def test_simulator_happy_path_game_start(self):
        sdk = GameMachineSDK(str(self.machine.id), self.secret, client=self.client)
        qr = qr_service.generate_qr_token(self.user, 'dev-1')['token']

        # Identify
        id_res = sdk.identify(qr)
        self.assertTrue(id_res['eligible'])
        self.assertEqual(id_res['wallet_total_paise'], 10000)

        # Charge
        charge_res = sdk.charge(qr, 'TEKKEN', 'IDEM_SIM_HAPPY')
        self.assertTrue(charge_res['approved'])
        self.assertEqual(charge_res['charged_paise'], 5000)
        self.assertEqual(charge_res['balance_after_paise'], 5000)

        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 5000)

    def test_simulator_insufficient_funds_decline(self):
        poor_user = User.objects.create_user(phone_number='+919111111111', pin='123456')
        sdk = GameMachineSDK(str(self.machine.id), self.secret, client=self.client)
        qr = qr_service.generate_qr_token(poor_user, 'dev-2')['token']

        charge_res = sdk.charge(qr, 'TEKKEN', 'IDEM_SIM_POOR')
        self.assertFalse(charge_res.get('approved', False))

    def test_simulator_hardware_fault_refund(self):
        sdk = GameMachineSDK(str(self.machine.id), self.secret, client=self.client)
        qr = qr_service.generate_qr_token(self.user, 'dev-3')['token']

        charge_res = sdk.charge(qr, 'TEKKEN', 'IDEM_SIM_FAULT')
        self.assertTrue(charge_res['approved'])
        txn_id = charge_res['transaction_id']

        # Refund
        refund_res = sdk.refund(txn_id, 'RELAY_JAMMED')
        self.assertTrue(refund_res['refunded'])

        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 10000) # Restored back to 100 INR!

    def test_simulator_single_use_jti_rejection(self):
        qr = qr_service.generate_qr_token(self.user, 'dev-4')['token']
        sdk = GameMachineSDK(str(self.machine.id), self.secret, client=self.client)

        res1 = sdk.charge(qr, 'TEKKEN', 'IDEM_1')
        self.assertTrue(res1['approved'])

        res2 = sdk.charge(qr, 'TEKKEN', 'IDEM_2')
        self.assertFalse(res2.get('approved', False))

        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 5000) # Only 1 charge deducted
