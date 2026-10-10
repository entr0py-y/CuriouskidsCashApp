from django.dispatch import Signal

# Signal fired when a user completes signup.
# The wallet service will listen to this to create the initial wallet ledger row.
user_created = Signal()
