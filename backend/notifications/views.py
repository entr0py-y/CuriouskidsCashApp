from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from notifications.serializers import RegisterDeviceTokenSerializer
from notifications.services import notification_service

class RegisterDeviceTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = RegisterDeviceTokenSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        notification_service.register_fcm_token(
            user=request.user,
            token=serializer.validated_data['token'],
            platform=serializer.validated_data.get('platform', 'ANDROID')
        )
        return Response({"status": "REGISTERED"})
