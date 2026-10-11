from django.db import transaction
from offers.models import BonusOffer, CouponCode, Referral
from wallets.services import ledger
from core_api.clock import Clock

def evaluate_recharge_bonus(user, amount_paise: int, idempotency_key: str):
    """
    Evaluates applicable recharge bonus offer and grants bonus bucket funds via bank ledger.
    """
    now = Clock.now()
    offer = BonusOffer.objects.filter(
        min_topup_paise__lte=amount_paise,
        valid_from__lte=now,
        valid_until__gte=now,
        is_active=True
    ).order_by('-bonus_paise').first()

    if not offer:
        return None

    bonus_paise = offer.bonus_paise
    ledger.credit_topup(
        user=user,
        amount_paise=bonus_paise,
        source='GATEWAY', # Granted to bonus bucket
        idempotency_key=f"BONUS:{offer.id}:{idempotency_key}",
        reference_id=str(offer.id)
    )
    return bonus_paise

def evaluate_referral_reward(referred_user):
    """
    Grants referral reward to referrer and referred user on referred user's first topup.
    """
    ref = Referral.objects.filter(referred=referred_user, status='PENDING').first()
    if not ref:
        return

    with transaction.atomic():
        ref.status = 'REWARDED'
        ref.save()

        # Reward referrer
        ledger.credit_topup(
            user=ref.referrer,
            amount_paise=ref.referrer_reward_paise,
            source='GATEWAY',
            idempotency_key=f"REF_REWARD_REFERRER:{ref.id}",
            reference_id=str(ref.id)
        )

        # Reward referred user
        ledger.credit_topup(
            user=ref.referred,
            amount_paise=ref.referred_reward_paise,
            source='GATEWAY',
            idempotency_key=f"REF_REWARD_REFERRED:{ref.id}",
            reference_id=str(ref.id)
        )
