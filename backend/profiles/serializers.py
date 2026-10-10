from rest_framework import serializers
from profiles.models import Branch, Profile, FamilyMember, ConsentLedger
from profiles.services.profile_service import calculate_profile_completion, calculate_age

class BranchSerializer(serializers.ModelSerializer):
    class Meta:
        model = Branch
        fields = ['id', 'name', 'address', 'latitude', 'longitude', 'opening_hours', 'contact_numbers', 'facilities', 'images', 'is_active']

class ProfileSerializer(serializers.ModelSerializer):
    completion_percentage = serializers.SerializerMethodField()
    is_minor = serializers.SerializerMethodField()

    class Meta:
        model = Profile
        fields = [
            'full_name', 'date_of_birth', 'email', 'gender', 'city',
            'preferred_branch', 'emergency_contact_name', 'emergency_contact_phone',
            'profile_photo_url', 'language_preference', 'notification_preferences',
            'guardian_name', 'guardian_phone', 'guardian_relationship', 'parental_consent_given',
            'completion_percentage', 'is_minor'
        ]

    def get_completion_percentage(self, obj):
        return calculate_profile_completion(obj)

    def get_is_minor(self, obj):
        if obj.date_of_birth:
            return calculate_age(obj.date_of_birth) < 18
        return False

class FamilyMemberSerializer(serializers.ModelSerializer):
    age = serializers.SerializerMethodField()

    class Meta:
        model = FamilyMember
        fields = ['id', 'name', 'date_of_birth', 'profile_photo_url', 'age']
        read_only_fields = ['id', 'parent_user']

    def get_age(self, obj):
        return calculate_age(obj.date_of_birth)

class ConsentLedgerSerializer(serializers.ModelSerializer):
    class Meta:
        model = ConsentLedger
        fields = ['id', 'document_type', 'document_version', 'created_at']
        read_only_fields = ['id', 'created_at']
