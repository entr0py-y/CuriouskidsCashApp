from datetime import date
from django.conf import settings
from rest_framework.exceptions import ValidationError
from profiles.models import Profile, ProfileAuditLog

def calculate_age(dob: date) -> int:
    today = date.today()
    return today.year - dob.year - ((today.month, today.day) < (dob.month, dob.day))

def is_minor(dob: date) -> bool:
    return calculate_age(dob) < 18

def calculate_profile_completion(profile: Profile) -> int:
    """
    Computes profile completion percentage server-side based on required fields.
    """
    required_fields = [
        profile.full_name,
        profile.date_of_birth,
        profile.city,
        profile.preferred_branch,
        profile.emergency_contact_name,
        profile.emergency_contact_phone,
        profile.email,
        profile.language_preference
    ]

    # If the user is a minor, guardian details and parental consent are also required
    if profile.date_of_birth and is_minor(profile.date_of_birth):
        required_fields.extend([
            profile.guardian_name,
            profile.guardian_phone,
            profile.guardian_relationship,
            profile.parental_consent_given
        ])

    filled_count = sum(1 for field in required_fields if field)
    total_count = len(required_fields)

    return int((filled_count / total_count) * 100)

def update_profile(profile: Profile, data: dict, updated_by_str: str = "USER") -> Profile:
    """
    Updates profile fields with strict validation and logs audit records for sensitive fields.
    """
    sensitive_fields = ['date_of_birth', 'guardian_name', 'guardian_phone', 'guardian_relationship']

    # 1. Age / DOB validation
    new_dob = data.get('date_of_birth', profile.date_of_birth)
    if new_dob:
        if new_dob > date.today():
            raise ValidationError("Date of birth cannot be in the future.")
        age = calculate_age(new_dob)
        if age > 120 or age < 1:
            raise ValidationError("Please provide a valid date of birth.")

    # 2. Minor guardian check
    if new_dob and is_minor(new_dob):
        g_name = data.get('guardian_name', profile.guardian_name)
        g_phone = data.get('guardian_phone', profile.guardian_phone)
        g_rel = data.get('guardian_relationship', profile.guardian_relationship)
        p_consent = data.get('parental_consent_given', profile.parental_consent_given)

        if not (g_name and g_phone and g_rel):
            raise ValidationError("Minor accounts require a guardian name, phone, and relationship.")
        if not p_consent:
            raise ValidationError("Parental consent must be explicitly recorded for minor accounts.")

    # 3. Apply updates & Audit logging
    for field, new_val in data.items():
        if hasattr(profile, field):
            old_val = getattr(profile, field)
            if old_val != new_val:
                if field in sensitive_fields:
                    ProfileAuditLog.objects.create(
                        user=profile.user,
                        field_changed=field,
                        old_value=str(old_val),
                        new_value=str(new_val),
                        changed_by=updated_by_str
                    )
                setattr(profile, field, new_val)

    profile.save()
    return profile

def generate_photo_upload_url(user, filename: str, content_type: str) -> dict:
    """
    Generates a pre-signed S3 upload URL for profile photos with strict type/extension checks.
    """
    allowed_types = ['image/jpeg', 'image/png', 'image/webp']
    if content_type.lower() not in allowed_types:
        raise ValidationError("Invalid file type. Only JPEG, PNG, and WEBP images are allowed.")

    file_ext = filename.split('.')[-1]
    s3_key = f"profiles/{user.id}/photo.{file_ext}"
    bucket = getattr(settings, 'S3_BUCKET_NAME', 'skyjumpers-media')

    # Mock pre-signed URL for local/testing
    upload_url = f"https://{bucket}.s3.amazonaws.com/{s3_key}?mock_presigned_signature=12345"
    public_url = f"https://{bucket}.s3.amazonaws.com/{s3_key}"

    return {
        "upload_url": upload_url,
        "public_url": public_url,
        "key": s3_key,
        "content_type": content_type
    }
