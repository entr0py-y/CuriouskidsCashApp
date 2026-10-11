from django.urls import path
from bookings.views import (
    ProductListView, TimeSlotListView, WaiverDocumentListView,
    SignWaiverView, MyWaiversView, CreateBookingView, PayBookingView, CancelBookingView
)

urlpatterns = [
    path('products', ProductListView.as_view(), name='product-list'),
    path('slots', TimeSlotListView.as_view(), name='slot-list'),
    path('waivers/documents', WaiverDocumentListView.as_view(), name='waiver-documents'),
    path('waivers/sign', SignWaiverView.as_view(), name='waiver-sign'),
    path('waivers/my-waivers', MyWaiversView.as_view(), name='my-waivers'),
    path('bookings', CreateBookingView.as_view(), name='booking-create'),
    path('bookings/<uuid:pk>/pay', PayBookingView.as_view(), name='booking-pay'),
    path('bookings/<uuid:pk>/cancel', CancelBookingView.as_view(), name='booking-cancel'),
]
