from rest_framework import serializers
from adminpanel.models import AuditLog, MakerCheckerRequest, CustomerSupportNote

class RevealPiiSerializer(serializers.Serializer):
    reason = serializers.CharField()

class GoodwillCreditSerializer(serializers.Serializer):
    amount_paise = serializers.IntegerField(min_value=100)
    reason = serializers.CharField()

class ApproveMakerCheckerSerializer(serializers.Serializer):
    is_approved = serializers.BooleanField()
    reason = serializers.CharField()

class SuspendUserSerializer(serializers.Serializer):
    suspend = serializers.BooleanField()
    reason = serializers.CharField()

class AuditLogSerializer(serializers.ModelSerializer):
    class Meta:
        model = AuditLog
        fields = ['id', 'staff_user', 'action_type', 'target_model', 'target_id', 'reason', 'ip_address', 'created_at']

class MakerCheckerRequestSerializer(serializers.ModelSerializer):
    class Meta:
        model = MakerCheckerRequest
        fields = ['id', 'maker', 'checker', 'action_type', 'target_user', 'amount_paise', 'reason', 'status', 'created_at']
