from django.http import HttpResponse
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.generics import ListAPIView
from adminpanel.models import AuditLog, MakerCheckerRequest
from adminpanel.serializers import (
    RevealPiiSerializer, GoodwillCreditSerializer, ApproveMakerCheckerSerializer,
    SuspendUserSerializer, AuditLogSerializer, MakerCheckerRequestSerializer
)
from adminpanel.services import customer_care_service, reports_service, staff_service
from core_api.utils import get_client_ip

class CustomerSearchView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        query = request.query_params.get('q', '')
        results = customer_care_service.search_customers(request.user, query)
        return Response({"results": results})

class RevealPiiView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, customer_id):
        serializer = RevealPiiSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = customer_care_service.reveal_pii(
            staff_user=request.user,
            customer_id=str(customer_id),
            reason=serializer.validated_data['reason'],
            ip=get_client_ip(request)
        )
        return Response(res)

class GoodwillCreditView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, customer_id):
        serializer = GoodwillCreditSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = customer_care_service.adjust_wallet_goodwill(
            staff_user=request.user,
            customer_id=str(customer_id),
            amount_paise=serializer.validated_data['amount_paise'],
            reason=serializer.validated_data['reason'],
            ip=get_client_ip(request)
        )
        return Response(res)

class ApproveMakerCheckerView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, request_id):
        serializer = ApproveMakerCheckerSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = customer_care_service.approve_maker_checker_request(
            checker_user=request.user,
            request_id=str(request_id),
            is_approved=serializer.validated_data['is_approved'],
            reason=serializer.validated_data['reason'],
            ip=get_client_ip(request)
        )
        return Response(res)

class SuspendUserView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, customer_id):
        serializer = SuspendUserSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = customer_care_service.toggle_user_suspension(
            staff_user=request.user,
            customer_id=str(customer_id),
            suspend=serializer.validated_data['suspend'],
            reason=serializer.validated_data['reason'],
            ip=get_client_ip(request)
        )
        return Response(res)

class AuditLogListView(ListAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = AuditLogSerializer

    def get_queryset(self):
        staff_service.verify_staff_permission(self.request.user, 'REPORTS')
        return AuditLog.objects.all()

class WalletLiabilityReportView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        res = reports_service.get_wallet_liability_report(request.user)
        return Response(res)

class DailyCollectionsCsvView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        csv_data = reports_service.generate_daily_collections_csv(request.user)
        response = HttpResponse(csv_data, content_type='text/csv')
        response['Content-Disposition'] = 'attachment; filename="daily_collections.csv"'
        return response
