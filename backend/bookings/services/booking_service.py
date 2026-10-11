import datetime
import logging
from django.db import transaction
from django.conf import settings
from rest_framework.exceptions import ValidationError
from bookings.models import Product, TimeSlot, Booking, BookingItem, BookingParticipant, SignedWaiver, PartyDetails
from wallets.services import ledger
from core_api.clock import Clock

logger = logging.getLogger(__name__)

def create_booking_reservation(user, branch_id: str, slot_id: str, items_data: list, participants_data: list, idempotency_key: str, party_data: dict = None) -> Booking:
    """
    Holds time slot capacity with a 10-minute reservation.
    Uses pessimistic row locking (select_for_update) to prevent overselling.
    """
    existing = Booking.objects.filter(idempotency_key=idempotency_key).first()
    if existing:
        return existing

    participants_count = len(participants_data)
    if participants_count < 1:
        raise ValidationError("At least one participant is required.")

    with transaction.atomic():
        # Lock time slot row to prevent concurrent overselling
        slot = TimeSlot.objects.select_for_update().filter(id=slot_id, branch_id=branch_id).first()
        if not slot:
            raise ValidationError("INVALID_SLOT: Time slot not found.")

        if slot.available_seats < participants_count:
            raise ValidationError("SLOT_FULL: Selected time slot does not have enough capacity.")

        # Calculate items total
        total_paise = 0
        booking_items = []
        for item in items_data:
            product = Product.objects.filter(id=item['product_id'], is_active=True).first()
            if not product:
                raise ValidationError(f"INVALID_PRODUCT: Product {item['product_id']} not found.")
            qty = item.get('quantity', 1)
            line_paise = product.price_paise * qty
            total_paise += line_paise
            booking_items.append((product, qty, product.price_paise))

        expires_at = Clock.now() + datetime.timedelta(minutes=10)
        booking = Booking.objects.create(
            user=user,
            branch_id=branch_id,
            slot=slot,
            total_paise=total_paise,
            status='RESERVED',
            idempotency_key=idempotency_key,
            expires_at=expires_at
        )

        for product, qty, price in booking_items:
            BookingItem.objects.create(booking=booking, product=product, quantity=qty, price_paise=price)

        for p in participants_data:
            BookingParticipant.objects.create(
                booking=booking,
                name=p['name'],
                is_minor=p.get('is_minor', False)
            )

        if party_data:
            PartyDetails.objects.create(
                booking=booking,
                child_name=party_data['child_name'],
                child_age=party_data['child_age'],
                guest_count=party_data['guest_count'],
                notes=party_data.get('notes', ''),
                deposit_paid_paise=party_data.get('deposit_paid_paise', 0)
            )

        # Increment slot reservation count
        slot.reserved_count += participants_count
        slot.save()

    return booking

def pay_and_confirm_booking(user, booking_id: str) -> Booking:
    """
    Atomic transaction:
    1. Verifies signed waivers for all participants.
    2. Debits user wallet via bank ledger.
    3. Confirms booking, increments slot.confirmed_count, generates gate entry pass.
    """
    booking = Booking.objects.filter(id=booking_id, user=user).first()
    if not booking:
        raise ValidationError("BOOKING_NOT_FOUND")

    if booking.status == 'CONFIRMED':
        return booking

    if booking.status == 'CANCELLED' or Clock.now() > booking.expires_at:
        raise ValidationError("BOOKING_EXPIRED: Reservation expired or cancelled.")

    participants = booking.participants.all()
    today = Clock.now().date()

    # 1. Waiver Gate Verification
    for p in participants:
        valid_waiver = SignedWaiver.objects.filter(
            user=user,
            participant_name__iexact=p.name,
            valid_until__gte=today
        ).first()
        if not valid_waiver:
            raise ValidationError(f"WAIVER_REQUIRED: Valid waiver required for participant {p.name}")
        p.waiver = valid_waiver
        p.save()

    with transaction.atomic():
        # 2. Debit Wallet in bank ledger
        ledger.debit_spend(
            user=user,
            amount_paise=booking.total_paise,
            reason='BOOKING',
            idempotency_key=f"BOOKING_PAY:{booking.id}",
            reference_id=str(booking.id)
        )

        # 3. Confirm Booking & update TimeSlot counters
        slot = TimeSlot.objects.select_for_update().get(id=booking.slot.id)
        slot.reserved_count = max(0, slot.reserved_count - len(participants))
        slot.confirmed_count += len(participants)
        slot.save()

        booking.status = 'CONFIRMED'
        booking.entry_pass_code = f"SKYJ1:GATE:{booking.id}"
        booking.save()

    return booking

def cancel_booking(user, booking_id: str) -> Booking:
    """
    Cancels booking and issues a REVERSAL transaction crediting money back to wallet.
    """
    booking = Booking.objects.filter(id=booking_id, user=user).first()
    if not booking or booking.status == 'CANCELLED':
        raise ValidationError("BOOKING_NOT_FOUND")

    with transaction.atomic():
        if booking.status == 'CONFIRMED':
            # Reverse ledger transaction
            ledger.credit_topup(
                user=user,
                amount_paise=booking.total_paise,
                source='REVERSAL',
                idempotency_key=f"CANCEL_BOOKING:{booking.id}",
                reference_id=str(booking.id)
            )
            slot = TimeSlot.objects.select_for_update().get(id=booking.slot.id)
            slot.confirmed_count = max(0, slot.confirmed_count - booking.participants.count())
            slot.save()

        booking.status = 'CANCELLED'
        booking.save()

    return booking
