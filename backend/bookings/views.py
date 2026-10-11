from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.generics import ListAPIView
from bookings.models import Product, TimeSlot, WaiverDocument, SignedWaiver, Booking
from bookings.serializers import (
    ProductSerializer, TimeSlotSerializer, WaiverDocumentSerializer,
    SignedWaiverSerializer, SignWaiverRequestSerializer,
    CreateBookingRequestSerializer, BookingSerializer
)
from bookings.services import booking_service, waiver_service
from core_api.utils import get_client_ip

class ProductListView(ListAPIView):
    permission_classes = [AllowAny]
    serializer_class = ProductSerializer

    def get_queryset(self):
        branch_id = self.request.query_params.get('branch_id')
        qs = Product.objects.filter(is_active=True)
        if branch_id:
            qs = qs.filter(branch_id=branch_id)
        return qs

class TimeSlotListView(ListAPIView):
    permission_classes = [AllowAny]
    serializer_class = TimeSlotSerializer

    def get_queryset(self):
        branch_id = self.request.query_params.get('branch_id')
        date_str = self.request.query_params.get('date')
        qs = TimeSlot.objects.all()
        if branch_id:
            qs = qs.filter(branch_id=branch_id)
        if date_str:
            qs = qs.filter(date=date_str)
        return qs

class WaiverDocumentListView(ListAPIView):
    permission_classes = [AllowAny]
    serializer_class = WaiverDocumentSerializer

    def get_queryset(self):
        branch_id = self.request.query_params.get('branch_id')
        qs = WaiverDocument.objects.filter(is_active=True)
        if branch_id:
            qs = qs.filter(branch_id=branch_id)
        return qs

class SignWaiverView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = SignWaiverRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        waiver = waiver_service.sign_waiver(
            user=request.user,
            participant_name=serializer.validated_data['participant_name'],
            is_minor=serializer.validated_data['is_minor'],
            guardian_name=serializer.validated_data.get('guardian_name', ''),
            guardian_phone=serializer.validated_data.get('guardian_phone', ''),
            document_id=serializer.validated_data['document_id'],
            signature_data=serializer.validated_data['signature_data'],
            ip=get_client_ip(request),
            device_id=request.data.get('device_id', '')
        )
        return Response(SignedWaiverSerializer(waiver).data)

class MyWaiversView(ListAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = SignedWaiverSerializer

    def get_queryset(self):
        return SignedWaiver.objects.filter(user=self.request.user)

class CreateBookingView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = CreateBookingRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        booking = booking_service.create_booking_reservation(
            user=request.user,
            branch_id=serializer.validated_data['branch_id'],
            slot_id=serializer.validated_data['slot_id'],
            items_data=serializer.validated_data['items'],
            participants_data=serializer.validated_data['participants'],
            idempotency_key=serializer.validated_data['idempotency_key'],
            party_data=serializer.validated_data.get('party_details')
        )
        return Response(BookingSerializer(booking).data)

class PayBookingView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        booking = booking_service.pay_and_confirm_booking(user=request.user, booking_id=str(pk))
        return Response(BookingSerializer(booking).data)

class CancelBookingView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        booking = booking_service.cancel_booking(user=request.user, booking_id=str(pk))
        return Response(BookingSerializer(booking).data)
