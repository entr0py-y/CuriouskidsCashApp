import uuid
from django.db import models
from django.db.models import F, Q
from django.conf import settings
from core_api.models import TimestampedModel

class LedgerAccount(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    type = models.CharField(max_length=50) # e.g. USER_WALLET_MAIN, USER_WALLET_BONUS, GATEWAY_CLEARING, REVENUE
    owner_user = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.PROTECT)
    currency = models.CharField(max_length=3, default='INR')

    def __str__(self):
        return f"{self.type} - {self.id}"

class LedgerTransaction(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    type = models.CharField(max_length=50) # e.g. TOPUP_GATEWAY, SPEND_MACHINE, REVERSAL
    idempotency_key = models.CharField(max_length=255, unique=True, db_index=True)
    status = models.CharField(max_length=20, default='POSTED')
    reference_type = models.CharField(max_length=50)
    reference_id = models.CharField(max_length=255)
    metadata = models.JSONField(default=dict)
    created_by = models.CharField(max_length=255, blank=True)

class LedgerEntry(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    transaction = models.ForeignKey(LedgerTransaction, related_name='entries', on_delete=models.PROTECT)
    account = models.ForeignKey(LedgerAccount, on_delete=models.PROTECT)
    direction = models.CharField(max_length=10) # DEBIT, CREDIT
    amount_paise = models.BigIntegerField()

    class Meta:
        constraints = [
            models.CheckConstraint(check=Q(amount_paise__gt=0), name='entry_amount_positive'),
            models.CheckConstraint(check=Q(direction__in=['DEBIT', 'CREDIT']), name='valid_direction'),
        ]

class Wallet(TimestampedModel):
    user = models.OneToOneField(settings.AUTH_USER_MODEL, on_delete=models.PROTECT, related_name='wallet')
    main_balance = models.BigIntegerField(default=0)
    bonus_balance = models.BigIntegerField(default=0)
    version = models.BigIntegerField(default=1)

    main_account = models.OneToOneField(LedgerAccount, on_delete=models.PROTECT, related_name='wallet_main')
    bonus_account = models.OneToOneField(LedgerAccount, on_delete=models.PROTECT, related_name='wallet_bonus')

    class Meta:
        constraints = [
            models.CheckConstraint(check=Q(main_balance__gte=0), name='main_balance_non_negative'),
            models.CheckConstraint(check=Q(bonus_balance__gte=0), name='bonus_balance_non_negative'),
        ]
