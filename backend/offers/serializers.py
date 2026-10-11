from rest_framework import serializers
from offers.models import BonusOffer

class BonusOfferSerializer(serializers.ModelSerializer):
    class Meta:
        model = BonusOffer
        fields = ['id', 'name', 'min_topup_paise', 'bonus_paise', 'valid_from', 'valid_until', 'is_first_topup_only']
