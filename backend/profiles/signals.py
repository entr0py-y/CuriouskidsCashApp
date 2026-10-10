from django.dispatch import receiver
from accounts.signals import user_created
from profiles.models import Profile

@receiver(user_created)
def create_profile_for_new_user(sender, user, **kwargs):
    """
    Automatically initializes a Profile instance whenever a new User signs up.
    """
    Profile.objects.get_or_create(user=user)
