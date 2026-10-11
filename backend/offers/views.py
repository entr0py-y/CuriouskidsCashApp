from rest_framework.generics import ListAPIView
from rest_framework.permissions import AllowAny
from offers.models import BonusOffer
from offers.serializers import BonusOfferSerializer
from core_api.clock import Clock

class BonusOfferListView(ListAPIView):
    permission_classes = [AllowAny]
    serializer_class = BonusOfferSerializer

    def get_queryset(self):
        now = Clock.now()
        return BonusOffer.objects.filter(valid_from__lte=now, valid_until__gte=now, is_active=True)
