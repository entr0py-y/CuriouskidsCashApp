import datetime
from rest_framework.exceptions import ValidationError
from bookings.models import WaiverDocument, SignedWaiver
from core_api.clock import Clock

def sign_waiver(user, participant_name: str, is_minor: bool, guardian_name: str, guardian_phone: str, document_id: str, signature_data: str, ip: str, device_id: str) -> SignedWaiver:
    """
    Signs a digital liability waiver for a participant.
    Minors require guardian_name and guardian_phone.
    Valid for 12 months.
    """
    doc = WaiverDocument.objects.filter(id=document_id, is_active=True).first()
    if not doc:
        raise ValidationError("Waiver document not found or inactive.")

    if is_minor and (not guardian_name or not guardian_phone):
        raise ValidationError("Guardian details (name & phone) are required for minor waivers.")

    valid_until = Clock.now().date() + datetime.timedelta(days=365) # 12 months

    waiver = SignedWaiver.objects.create(
        user=user,
        participant_name=participant_name,
        is_minor=is_minor,
        guardian_name=guardian_name if is_minor else '',
        guardian_phone=guardian_phone if is_minor else '',
        document=doc,
        signature_data=signature_data,
        valid_until=valid_until,
        ip_address=ip,
        device_id=device_id
    )

    return waiver
