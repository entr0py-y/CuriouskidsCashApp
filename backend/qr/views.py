from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from qr.services import qr_service
from qr.crypto import get_jwks

class QrTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        device_id = request.headers.get('X-Device-ID', request.device.device_id if hasattr(request, 'device') else 'unknown')
        res = qr_service.generate_qr_token(request.user, device_id)
        return Response(res)

class QrKeysView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        return Response(get_jwks())
