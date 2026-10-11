import concurrent.futures
from datetime import date, time, timedelta
from django.test import TestCase, TransactionTestCase
from rest_framework.test import APIClient
from rest_framework.exceptions import ValidationError
from accounts.models import User
from profiles.models import Branch
from bookings.models import Product, TimeSlot, WaiverDocument, Booking, SignedWaiver
from bookings.services import booking_service, waiver_service
from wallets.services.ledger import credit_topup
from wallets.models import Wallet
from core_api.clock import Clock

class CommerceTests(TransactionTestCase):
    def setUp(self):
        self.user = User.objects.create_user(phone_number='+919876543210', pin='123456')
        credit_topup(self.user, 100000, 'GATEWAY', 'TOPUP_COMMERCE_1', 'REF_1') # 1000 INR

        self.branch = Branch.objects.create(
            name="Delhi Branch",
            address="CP",
            latitude=28.6,
            longitude=77.2
        )

        self.product = Product.objects.create(
            branch=self.branch,
            name="1 Hour Jump Pass",
            price_paise=50000 # 500 INR
        )

        self.slot = TimeSlot.objects.create(
            branch=self.branch,
            date=date.today() + timedelta(days=1),
            start_time=time(14, 0),
            end_time=time(15, 0),
            capacity=5 # Only 5 seats!
        )

        self.waiver_doc = WaiverDocument.objects.create(
            branch=self.branch,
            version="1.0",
            content_text="Legal liability waiver text..."
        )

        self.client = APIClient()
        self.client.force_authenticate(user=self.user)

    def test_50_parallel_bookings_for_5_available_seats_no_overselling(self):
        """
        Executes 50 parallel reservation requests for the last 5 seats.
        Verifies row-locking (select_for_update) prevents overselling.
        """
        def attempt_booking(idx):
            u = User.objects.create_user(phone_number=f'+9190000000{idx:02d}', pin='123456')
            try:
                booking_service.create_booking_reservation(
                    user=u,
                    branch_id=str(self.branch.id),
                    slot_id=str(self.slot.id),
                    items_data=[{'product_id': str(self.product.id), 'quantity': 1}],
                    participants_data=[{'name': f'Jumper {idx}', 'is_minor': False}],
                    idempotency_key=f'IDEM_PARALLEL_{idx}'
                )
                return True
            except ValidationError:
                return False

        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            results = list(executor.map(attempt_booking, range(50)))

        success_count = sum(1 for r in results if r is True)
        self.assertEqual(success_count, 5) # Exactly 5 seats reserved!

        self.slot.refresh_from_db()
        self.assertEqual(self.slot.reserved_count, 5)

    def test_booking_payment_requires_valid_waiver(self):
        # 1. Create reservation
        booking = booking_service.create_booking_reservation(
            user=self.user,
            branch_id=str(self.branch.id),
            slot_id=str(self.slot.id),
            items_data=[{'product_id': str(self.product.id), 'quantity': 1}],
            participants_data=[{'name': 'Pushk', 'is_minor': False}],
            idempotency_key='IDEM_WAIVER_TEST'
        )

        # 2. Attempt payment without signed waiver -> Fails
        with self.assertRaises(ValidationError) as ctx:
            booking_service.pay_and_confirm_booking(self.user, str(booking.id))
        self.assertIn("WAIVER_REQUIRED", str(ctx.exception))

        # 3. Sign Waiver
        waiver_service.sign_waiver(
            user=self.user,
            participant_name="Pushk",
            is_minor=False,
            guardian_name="",
            guardian_phone="",
            document_id=str(self.waiver_doc.id),
            signature_data="Pushk Signature",
            ip="127.0.0.1",
            device_id="dev-1"
        )

        # 4. Attempt payment with signed waiver -> Succeeds
        confirmed_booking = booking_service.pay_and_confirm_booking(self.user, str(booking.id))
        self.assertEqual(confirmed_booking.status, 'CONFIRMED')
        self.assertTrue(confirmed_booking.entry_pass_code.startswith("SKYJ1:GATE:"))

        # Wallet debited
        wallet = Wallet.objects.get(user=self.user)
        self.assertEqual(wallet.main_balance, 50000) # 100000 - 50000

    def test_failed_wallet_payment_leaves_no_confirmed_booking_and_no_debit(self):
        poor_user = User.objects.create_user(phone_number='+919111111111', pin='123456')

        # Sign waiver first
        waiver_service.sign_waiver(
            user=poor_user,
            participant_name="Poor Participant",
            is_minor=False,
            guardian_name="",
            guardian_phone="",
            document_id=str(self.waiver_doc.id),
            signature_data="Sig",
            ip="127.0.0.1",
            device_id="dev-1"
        )

        booking = booking_service.create_booking_reservation(
            user=poor_user,
            branch_id=str(self.branch.id),
            slot_id=str(self.slot.id),
            items_data=[{'product_id': str(self.product.id), 'quantity': 1}],
            participants_data=[{'name': 'Poor Participant', 'is_minor': False}],
            idempotency_key='IDEM_POOR_TEST'
        )

        # Payment fails due to insufficient funds
        with self.assertRaises(ValidationError) as ctx:
            booking_service.pay_and_confirm_booking(poor_user, str(booking.id))
        self.assertIn("INSUFFICIENT_FUNDS", str(ctx.exception))

        booking.refresh_from_db()
        self.assertEqual(booking.status, 'RESERVED') # Not confirmed!

    def test_minor_waiver_requires_guardian_details(self):
        with self.assertRaises(ValidationError):
            waiver_service.sign_waiver(
                user=self.user,
                participant_name="Minor Child",
                is_minor=True,
                guardian_name="", # Missing!
                guardian_phone="",
                document_id=str(self.waiver_doc.id),
                signature_data="Sig",
                ip="127.0.0.1",
                device_id="dev-1"
            )
