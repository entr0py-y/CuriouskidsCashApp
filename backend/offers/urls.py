from django.urls import path
from offers.views import BonusOfferListView

urlpatterns = [
    path('offers', BonusOfferListView.as_view(), name='offer-list'),
]
