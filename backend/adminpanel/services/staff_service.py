from rest_framework.exceptions import PermissionDenied
from adminpanel.models import StaffProfile, AuditLog

ROLE_PERMISSIONS = {
    'SUPER_ADMIN': ['ALL'],
    'FINANCE': ['VIEW_CUSTOMER', 'REPORTS', 'MAKER_CHECKER_APPROVE', 'REFUND', 'RECONCILIATION'],
    'CUSTOMER_CARE': ['VIEW_CUSTOMER', 'PII_REVEAL', 'GOODWILL_CREDIT_SMALL', 'SUSPEND_USER', 'SUPPORT_NOTE'],
    'BRANCH_MANAGER': ['VIEW_BRANCH_MACHINES', 'VIEW_BRANCH_SLOTS', 'EDIT_GAME_PRICES'],
    'AUDITOR': ['VIEW_READONLY', 'REPORTS'],
}

def get_staff_profile(user) -> StaffProfile:
    staff = getattr(user, 'staff_profile', None)
    if not staff:
        raise PermissionDenied("Staff access required.")
    if not staff.is_2fa_enabled:
        raise PermissionDenied("Mandatory two-factor authentication required for staff accounts.")
    return staff

def verify_staff_permission(user, required_perm: str):
    staff = get_staff_profile(user)
    allowed_perms = ROLE_PERMISSIONS.get(staff.role, [])
    if 'ALL' in allowed_perms or required_perm in allowed_perms:
        return staff
    raise PermissionDenied(f"Role '{staff.role}' lacks permission '{required_perm}'.")

def log_audit_action(staff_user, action_type: str, target_model: str, target_id: str, reason: str, ip: str, before: dict = None, after: dict = None) -> AuditLog:
    """
    Writes an immutable audit log entry for every staff action.
    """
    return AuditLog.objects.create(
        staff_user=staff_user,
        action_type=action_type,
        target_model=target_model,
        target_id=str(target_id),
        reason=reason or "No reason provided",
        before_json=before or {},
        after_json=after or {},
        ip_address=ip
    )
