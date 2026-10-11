import uuid
from django.db import models
from django.conf import settings
from core_api.models import TimestampedModel

class BonusOffer(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    name = models.CharField(max_length=255)
    min_topup_paise = models.BigIntegerField()
    bonus_paise = models.BigIntegerField()
    valid_from = models.DateTimeField()
    valid_until = models.DateTimeField()
    per_user_limit = models.IntegerField(default=1)
    is_first_topup_only = models.BooleanField(default=False)
    is_active = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']

    def __str__(self):
        return f"{self.name} - Topup {self.min_topup_paise//100} get {self.bonus_paise//100} Bonus"

class CouponCode(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    code = models.CharField(max_length=50, unique=True)
    discount_paise = models.BigIntegerField(default=0)
    bonus_paise = models.BigIntegerField(default=0)
    max_uses = models.IntegerField(default=100)
    current_uses = models.IntegerField(default=0)
    valid_until = models.DateTimeField()
    is_active = models.BooleanField(default=True)

    class Meta:
        ordering = ['-created_at']

class Referral(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    referrer = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='referrals_sent')
    referred = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='referrals_received')
    status = models.CharField(max_length=20, default='PENDING') # PENDING, REWARDED
    referrer_reward_paise = models.BigIntegerField(default=10000) # 100 INR
    referred_reward_paise = models.BigIntegerField(default=5000)  # 50 INR

    class Meta:
        ordering = ['-created_at']
        unique_together = ('referrer', 'referred')
