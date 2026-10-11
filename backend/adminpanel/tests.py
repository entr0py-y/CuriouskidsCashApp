from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from rest_framework.exceptions import PermissionDenied, ValidationError
from accounts.models import User
from adminpanel.models import StaffProfile, AuditLog, MakerCheckerRequest
from adminpanel.services import customer_care_service, reports_service

class AdminPanelTests(TestCase):
    def setUp(self):
        # Customers
        self.customer1 = User.objects.create_user(phone_number='+919876543210', pin='123456')

        # Staff Users
        self.care_user = User.objects.create_user(phone_number='+919100000001', pin='123456')
        StaffProfile.objects.create(user=self.care_user, role='CUSTOMER_CARE', is_2fa_enabled=True)

        self.finance_user = User.objects.create_user(phone_number='+919100000002', pin='123456')
        StaffProfile.objects.create(user=self.finance_user, role='FINANCE', is_2fa_enabled=True)

        self.finance_user2 = User.objects.create_user(phone_number='+919100000003', pin='123456')
        StaffProfile.objects.create(user=self.finance_user2, role='FINANCE', is_2fa_enabled=True)

        self.client_care = APIClient()
        self.client_care.force_authenticate(user=self.care_user)

        self.client_finance = APIClient()
        self.client_finance.force_authenticate(user=self.finance_user)

    def test_permission_matrix_and_pii_reveal_audit(self):
        url_reveal = reverse('admin-reveal-pii', kwargs={'customer_id': self.customer1.id})

        # 1. Customer Care reveals PII
        resp = self.client_care.post(url_reveal, {'reason': 'Customer called support'}, format='json')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()['unmasked_phone'], '+919876543210')

        # 2. Immutable AuditLog created
        log = AuditLog.objects.filter(action_type='PII_REVEAL', target_id=str(self.customer1.id)).first()
        self.assertIsNotNone(log)
        self.assertEqual(log.staff_user, self.care_user)

    def test_audit_log_append_only_immutability(self):
        log = AuditLog.objects.create(
            staff_user=self.care_user,
            action_type="TEST_ACTION",
            target_model="User",
            target_id=str(self.customer1.id),
            reason="Testing immutability"
        )
        self.assertIsNotNone(log.id)

        # Attempt to modify -> Fails
        with self.assertRaises(ValueError):
            log.reason = "Modified"
            log.save()

        # Attempt to delete -> Fails
        with self.assertRaises(ValueError):
            log.delete()

    def test_maker_checker_self_approval_prohibited(self):
        # 1. Customer Care creates large goodwill credit request (> ₹500)
        res = customer_care_service.adjust_wallet_goodwill(
            staff_user=self.care_user,
            customer_id=str(self.customer1.id),
            amount_paise=100000, # ₹1000
            reason="Compensation for error",
            ip="127.0.0.1"
        )
        self.assertEqual(res['status'], 'MAKER_CHECKER_REQUIRED')
        request_id = res['request_id']

        # 2. Finance User 1 attempts self-approval on a request they created -> Test with Finance User 1 as maker
        req_finance = MakerCheckerRequest.objects.create(
            maker=self.finance_user,
            action_type="LARGE_GOODWILL_CREDIT",
            target_user=self.customer1,
            amount_paise=100000,
            reason="Testing self approval",
            status='PENDING'
        )

        with self.assertRaises(PermissionDenied) as ctx:
            customer_care_service.approve_maker_checker_request(
                checker_user=self.finance_user, # Same user as maker!
                request_id=str(req_finance.id),
                is_approved=True,
                reason="Self approval attempt",
                ip="127.0.0.1"
            )
        self.assertIn("SELF_APPROVAL_PROHIBITED", str(ctx.exception))

        # 3. Different Finance staff (Finance User 2) approves -> Succeeds
        res_approve = customer_care_service.approve_maker_checker_request(
            checker_user=self.finance_user2,
            request_id=str(req_finance.id),
            is_approved=True,
            reason="Approved after review",
            ip="127.0.0.1"
        )
        self.assertEqual(res_approve['status'], 'APPROVED')

    def test_wallet_liability_report(self):
        report = reports_service.get_wallet_liability_report(self.finance_user)
        self.assertIn("total_outstanding_liability_paise", report)
