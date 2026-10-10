from django.test import TestCase
from django.urls import reverse
from datetime import date
from rest_framework.test import APIClient
from rest_framework.exceptions import ValidationError
from accounts.models import User
from profiles.models import Branch, Profile, FamilyMember, ConsentLedger
from profiles.services import profile_service

class ProfileTests(TestCase):
    def setUp(self):
        self.user1 = User.objects.create_user(phone_number='+919876543210', pin='123456')
        self.user2 = User.objects.create_user(phone_number='+919123456789', pin='123456')

        self.client1 = APIClient()
        self.client1.force_authenticate(user=self.user1)

        self.client2 = APIClient()
        self.client2.force_authenticate(user=self.user2)

        self.branch = Branch.objects.create(
            name="Test Branch",
            address="Test Address",
            latitude=28.5,
            longitude=77.2
        )

    def test_branch_list_public(self):
        client = APIClient()
        resp = client.get(reverse('branch-list'))
        self.assertEqual(resp.status_code, 200)
        self.assertGreaterEqual(len(resp.json()['results']), 1)

    def test_profile_completion_and_minor_validation(self):
        profile = Profile.objects.get(user=self.user1)

        # Initial completion (language preference default is 'en', so 12%)
        completion = profile_service.calculate_profile_completion(profile)
        self.assertEqual(completion, 12)

        # Update as an Adult
        profile_service.update_profile(profile, {
            'full_name': 'Adult User',
            'date_of_birth': date(1995, 5, 15),
            'city': 'Delhi',
            'preferred_branch': self.branch,
            'emergency_contact_name': 'Friend',
            'emergency_contact_phone': '+919999999999',
            'email': 'adult@example.com',
            'language_preference': 'en'
        })
        self.assertEqual(profile_service.calculate_profile_completion(profile), 100)

        # Try updating DOB to a Minor without guardian -> Should Fail
        profile2 = Profile.objects.get(user=self.user2)
        with self.assertRaises(ValidationError):
            profile_service.update_profile(profile2, {
                'date_of_birth': date(2015, 1, 1)
            })

        # Update Minor with guardian details -> Should Pass
        profile_service.update_profile(profile2, {
            'full_name': 'Minor User',
            'date_of_birth': date(2015, 1, 1),
            'guardian_name': 'Parent User',
            'guardian_phone': '+919876543210',
            'guardian_relationship': 'Father',
            'parental_consent_given': True
        })
        self.assertTrue(profile_service.is_minor(profile2.date_of_birth))

    def test_family_member_permissions_isolation(self):
        child1 = FamilyMember.objects.create(
            parent_user=self.user1,
            name="Child One",
            date_of_birth=date(2018, 6, 1)
        )

        url = reverse('family-detail', kwargs={'pk': child1.id})
        resp = self.client2.get(url)
        self.assertEqual(resp.status_code, 404)

        resp_del = self.client2.delete(url)
        self.assertEqual(resp_del.status_code, 404)

        resp_user1 = self.client1.get(url)
        self.assertEqual(resp_user1.status_code, 200)
        self.assertEqual(resp_user1.json()['name'], "Child One")

    def test_consent_ledger_append_only(self):
        consent = ConsentLedger.objects.create(
            user=self.user1,
            document_type="TERMS",
            document_version="1.0"
        )
        self.assertIsNotNone(consent.id)

        with self.assertRaises(ValueError):
            consent.document_version = "2.0"
            consent.save()

        with self.assertRaises(ValueError):
            consent.delete()

    def test_photo_upload_url_restrictions(self):
        res = profile_service.generate_photo_upload_url(self.user1, "avatar.png", "image/png")
        self.assertIn("upload_url", res)

        with self.assertRaises(ValidationError):
            profile_service.generate_photo_upload_url(self.user1, "script.exe", "application/x-msdownload")

    def test_data_export(self):
        resp = self.client1.get(reverse('data-export'))
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertIn("account", data)
        self.assertIn("profile", data)
        self.assertIn("family_members", data)
        self.assertIn("consents", data)
