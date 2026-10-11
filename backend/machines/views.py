from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import AllowAny
from machines.authentication import MachineHMACAuthentication
from machines.serializers import (
    ActivateMachineSerializer, IdentifySerializer, ChargeSerializer,
    RefundSerializer, HeartbeatSerializer, StartCashSessionSerializer,
    ProcessNoteSerializer, CloseCashSessionSerializer
)
from machines.services import machine_service, cash_session_service

class ActivateMachineView(APIView):
    permission_classes = [AllowAny]
    throttle_classes = []

    def post(self, request):
        serializer = ActivateMachineSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        machine, secret = machine_service.activate_machine(serializer.validated_data['activation_code'])
        return Response({
            "machine_id": str(machine.id),
            "secret": secret
        })

class MachineBaseAPIView(APIView):
    authentication_classes = [MachineHMACAuthentication]
    permission_classes = []
    throttle_classes = []

class IdentifyView(MachineBaseAPIView):
    def post(self, request):
        serializer = IdentifySerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = machine_service.identify_user(request.machine, serializer.validated_data['qr_token'])
        return Response(res)

class ChargeView(MachineBaseAPIView):
    def post(self, request):
        serializer = ChargeSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = machine_service.charge_game(
            machine=request.machine,
            qr_token_str=serializer.validated_data['qr_token'],
            game_code=serializer.validated_data['game_code'],
            idempotency_key=serializer.validated_data['idempotency_key']
        )
        return Response(res)

class RefundView(MachineBaseAPIView):
    def post(self, request, transaction_id):
        serializer = RefundSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = machine_service.refund_charge(
            machine=request.machine,
            transaction_id=str(transaction_id),
            reason=serializer.validated_data['reason']
        )
        return Response(res)

class HeartbeatView(MachineBaseAPIView):
    def post(self, request):
        serializer = HeartbeatSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = machine_service.record_heartbeat(
            machine=request.machine,
            status=serializer.validated_data['status'],
            software_version=serializer.validated_data.get('software_version', ''),
            validator_status=serializer.validated_data.get('validator_status', {})
        )
        return Response(res)

# Cash Kiosk Session Views
class StartCashSessionView(MachineBaseAPIView):
    def post(self, request):
        serializer = StartCashSessionSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = cash_session_service.start_cash_session(
            machine=request.machine,
            qr_token_str=serializer.validated_data['qr_token'],
            idempotency_key=serializer.validated_data['idempotency_key']
        )
        return Response(res)

class ProcessNoteView(MachineBaseAPIView):
    def post(self, request, session_id):
        serializer = ProcessNoteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = cash_session_service.process_accepted_note(
            machine=request.machine,
            session_id=str(session_id),
            note_value=serializer.validated_data['note_value'],
            sequence=serializer.validated_data['sequence'],
            idempotency_key=serializer.validated_data['idempotency_key']
        )
        return Response(res)

class CloseCashSessionView(MachineBaseAPIView):
    def post(self, request, session_id):
        serializer = CloseCashSessionSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        res = cash_session_service.close_cash_session(
            machine=request.machine,
            session_id=str(session_id),
            reason=serializer.validated_data['reason'],
            notes_total_kiosk=serializer.validated_data['notes_total']
        )
        return Response(res)
