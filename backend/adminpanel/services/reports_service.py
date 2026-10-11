import csv
import io
from django.db.models import Sum
from wallets.models import Wallet, LedgerEntry
from payments.models import PaymentOrder
from adminpanel.services.staff_service import verify_staff_permission

def get_wallet_liability_report(staff_user) -> dict:
    """
    Returns total outstanding main and bonus balances across all active customer wallets.
    """
    verify_staff_permission(staff_user, 'REPORTS')

    aggregates = Wallet.objects.aggregate(
        total_main=Sum('main_balance'),
        total_bonus=Sum('bonus_balance')
    )

    total_main = aggregates['total_main'] or 0
    total_bonus = aggregates['total_bonus'] or 0

    return {
        "total_main_balance_paise": total_main,
        "total_bonus_balance_paise": total_bonus,
        "total_outstanding_liability_paise": total_main + total_bonus
    }

def generate_daily_collections_csv(staff_user) -> str:
    """
    Generates CSV export for daily payment order collections.
    """
    verify_staff_permission(staff_user, 'REPORTS')

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(['Payment Order ID', 'User ID', 'Amount (Paise)', 'Currency', 'Status', 'Provider Payment ID', 'Created At'])

    orders = PaymentOrder.objects.filter(status='PAID').order_by('-created_at')[:1000]
    for o in orders:
        writer.writerow([str(o.id), str(o.user.id), o.amount_paise, o.currency, o.status, o.provider_payment_id, o.created_at.isoformat()])

    return output.getvalue()
