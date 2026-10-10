from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.generics import ListAPIView, RetrieveAPIView, ListCreateAPIView, RetrieveUpdateDestroyAPIView
from rest_framework.exceptions import ValidationError
from profiles.models import Branch, Profile, FamilyMember, ConsentLedger
from profiles.serializers import BranchSerializer, ProfileSerializer, FamilyMemberSerializer, ConsentLedgerSerializer
from profiles.services import profile_service
from core_api.utils import get_client_ip

class BranchListView(ListAPIView):
    permission_classes = [AllowAny]
    serializer_class = BranchSerializer
    queryset = Branch.objects.filter(is_active=True)
    ordering = '-created_at'

class BranchDetailView(RetrieveAPIView):
    permission_classes = [AllowAny]
    serializer_class = BranchSerializer
    queryset = Branch.objects.filter(is_active=True)

class ProfileView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        profile, _ = Profile.objects.get_or_create(user=request.user)
        serializer = ProfileSerializer(profile)
        return Response(serializer.data)

    def patch(self, request):
        profile, _ = Profile.objects.get_or_create(user=request.user)
        serializer = ProfileSerializer(profile, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)

        updated_profile = profile_service.update_profile(
            profile=profile,
            data=serializer.validated_data,
            updated_by_str=str(request.user.id)
        )
        return Response(ProfileSerializer(updated_profile).data)

class CompletionView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        profile, _ = Profile.objects.get_or_create(user=request.user)
        completion = profile_service.calculate_profile_completion(profile)
        return Response({"completion_percentage": completion})

class PhotoUploadUrlView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        filename = request.data.get('filename')
        content_type = request.data.get('content_type')
        if not filename or not content_type:
            raise ValidationError("Both filename and content_type are required.")

        res = profile_service.generate_photo_upload_url(request.user, filename, content_type)
        return Response(res)

class FamilyMemberListCreateView(ListCreateAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = FamilyMemberSerializer
    ordering = '-created_at'

    def get_queryset(self):
        return FamilyMember.objects.filter(parent_user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(parent_user=self.request.user)

class FamilyMemberDetailView(RetrieveUpdateDestroyAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = FamilyMemberSerializer

    def get_queryset(self):
        return FamilyMember.objects.filter(parent_user=self.request.user)

class LegalDocumentsView(APIView):
    permission_classes = [AllowAny]

    def get(self, request):
        return Response({
            "terms_of_service": {"version": "1.0", "url": "https://skyjumpers.in/terms"},
            "privacy_policy": {"version": "1.0", "url": "https://skyjumpers.in/privacy"},
            "parental_consent_waiver": {"version": "1.0", "url": "https://skyjumpers.in/parental-consent"}
        })

class RecordConsentView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        serializer = ConsentLedgerSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        consent = ConsentLedger.objects.create(
            user=request.user,
            document_type=serializer.validated_data['document_type'],
            document_version=serializer.validated_data['document_version'],
            ip_address=get_client_ip(request),
            device_id=request.data.get('device_id', '')
        )
        return Response(ConsentLedgerSerializer(consent).data)

class DataExportView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        """User data export endpoint as required by privacy rules."""
        user = request.user
        profile, _ = Profile.objects.get_or_create(user=user)
        family = FamilyMember.objects.filter(parent_user=user)
        consents = ConsentLedger.objects.filter(user=user)

        data = {
            "account": {
                "id": str(user.id),
                "phone_number": user.phone_number,
                "created_at": user.created_at.isoformat()
            },
            "profile": ProfileSerializer(profile).data,
            "family_members": FamilyMemberSerializer(family, many=True).data,
            "consents": ConsentLedgerSerializer(consents, many=True).data
        }
        return Response(data)
