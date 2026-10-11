import datetime
import logging
from django.db import transaction, IntegrityError
from rest_framework.exceptions import ValidationError
from machines.models import Machine, CashSession, CashSessionNote
from accounts.models import User
from qr.services import qr_service
from wallets.services import ledger
from wallets.models import Wallet
from core_api.clock import Clock

logger = logging.getLogger(__name__)

ALLOWED_DENOMINATIONS = [5, 10, 20, 50, 100, 200, 500]

def start_cash_session(machine: Machine, qr_token_str: str, idempotency_key: str) -> dict:
    """
    Starts a cash top-up session at a cash kiosk.
    - Consumes single-use QR token.
    - Binds session to user and machine.
    """
    if machine.type != 'VENDING_CASH':
        raise ValidationError("WRONG_MACHINE_TYPE: Machine is not a Cash Kiosk.")

    existing = CashSession.objects.filter(idempotency_key=idempotency_key).first()
    if existing:
        return {
            "session_id": str(existing.id),
            "expires_at": existing.expires_at.isoformat(),
            "user_ref": str(existing.user.id)
        }

    # Decode QR Token & consume jti (Single-use)
    payload = qr_service.decode_and_verify_qr_token(qr_token_str)
    jti = payload["jti"]
    qr_service.consume_jti_for_spending(jti)

    user_ref = payload["user_ref"]
    user = User.objects.filter(id=user_ref, is_active=True).first()
    if not user or user.deleted_at is not None:
        raise ValidationError("USER_SUSPENDED")

    expires_at = Clock.now() + datetime.timedelta(seconds=60)
    session = CashSession.objects.create(
        machine=machine,
        user=user,
        idempotency_key=idempotency_key,
        status='ACTIVE',
        expires_at=expires_at
    )

    return {
        "session_id": str(session.id),
        "expires_at": expires_at.isoformat(),
        "user_ref": str(user.id)
    }

def process_accepted_note(machine: Machine, session_id: str, note_value: int, sequence: int, idempotency_key: str) -> dict:
    """
    Credits an accepted banknote to the user's wallet via double-entry ledger.
    - Enforces allowed denominations [5, 10, 20, 50, 100, 200, 500].
    - Idempotent per idempotency_key.
    - Late notes after session close are accepted and flagged for review.
    """
    session = CashSession.objects.filter(id=session_id, machine=machine).first()
    if not session:
        raise ValidationError("SESSION_NOT_FOUND")

    # 1. Denomination Check
    if note_value not in ALLOWED_DENOMINATIONS:
        session.flagged_for_review = True
        session.status = 'FLAGGED'
        session.save()
        logger.error(f"SECURITY_ALERT: Unknown note denomination {note_value} inserted at machine {machine.id}")
        raise ValidationError(f"INVALID_DENOMINATION: {note_value} INR is not a supported bill.")

    # 2. Check existing note idempotency
    existing_note = CashSessionNote.objects.filter(idempotency_key=idempotency_key).first()
    if existing_note:
        wallet = Wallet.objects.get(user=session.user)
        return {
            "approved": True,
            "note_value": existing_note.note_value,
            "sequence": existing_note.sequence,
            "new_balance_paise": wallet.main_balance + wallet.bonus_balance,
            "transaction_id": existing_note.transaction_id
        }

    # 3. Late Note Handling (if session is closed, accept cash & flag for review)
    if session.status in ['CLOSED', 'EXPIRED']:
        session.flagged_for_review = True
        session.save()
        logger.warning(f"LATE_NOTE_ACCEPTED: Note inserted into closed session {session.id}")

    amount_paise = note_value * 100

    with transaction.atomic():
        # Credit wallet through ledger
        txn = ledger.credit_topup(
            user=session.user,
            amount_paise=amount_paise,
            source='TOPUP_CASH_MACHINE',
            idempotency_key=idempotency_key,
            reference_id=f"KIOSK:{machine.id}:{session.id}:{sequence}"
        )

        try:
            CashSessionNote.objects.create(
                cash_session=session,
                sequence=sequence,
                note_value=note_value,
                idempotency_key=idempotency_key,
                transaction_id=str(txn.id)
            )
        except IntegrityError:
            pass # Idempotent replay

        session.notes_total_backend_paise += amount_paise
        session.save()

    wallet = Wallet.objects.get(user=session.user)
    new_balance = wallet.main_balance + wallet.bonus_balance

    return {
        "approved": True,
        "note_value": note_value,
        "sequence": sequence,
        "new_balance_paise": new_balance,
        "transaction_id": str(txn.id)
    }

def close_cash_session(machine: Machine, session_id: str, reason: str, notes_total_kiosk: int) -> dict:
    """
    Closes a cash session.
    Compares kiosk reported total vs backend credited total.
    Mismatch -> Flags session for review.
    """
    session = CashSession.objects.filter(id=session_id, machine=machine).first()
    if not session:
        raise ValidationError("SESSION_NOT_FOUND")

    if session.status == 'CLOSED':
        return {
            "status": "CLOSED",
            "session_id": str(session.id),
            "total_credited_paise": session.notes_total_backend_paise,
            "flagged_mismatch": session.flagged_for_review
        }

    session.close_reason = reason
    session.notes_total_kiosk = notes_total_kiosk

    # Check total mismatch (notes_total_kiosk is in Rupees or Paise)
    kiosk_paise = notes_total_kiosk * 100 if notes_total_kiosk < 10000 else notes_total_kiosk
    if kiosk_paise != session.notes_total_backend_paise:
        session.flagged_for_review = True
        session.status = 'FLAGGED'
        logger.error(f"MISMATCH_ALERT: Session {session.id} total mismatch! Kiosk: {kiosk_paise}, Backend: {session.notes_total_backend_paise}")
    else:
        session.status = 'CLOSED'

    session.save()

    return {
        "status": session.status,
        "session_id": str(session.id),
        "total_credited_paise": session.notes_total_backend_paise,
        "flagged_mismatch": session.flagged_for_review
    }
