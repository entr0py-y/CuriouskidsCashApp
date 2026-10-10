import uuid
from django.db import models
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from core_api.models import TimestampedModel

class UserManager(BaseUserManager):
    def create_user(self, phone_number, pin=None, **extra_fields):
        if not phone_number:
            raise ValueError("Phone number must be set")
        user = self.model(phone_number=phone_number, **extra_fields)
        if pin:
            user.set_password(pin)
        user.save(using=self._db)
        from accounts.signals import user_created
        user_created.send(sender=self.model, user=user)
        return user

    def create_superuser(self, phone_number, pin=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(phone_number, pin, **extra_fields)

class User(AbstractBaseUser, PermissionsMixin, TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    phone_number = models.CharField(max_length=15, unique=True, db_index=True)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    deleted_at = models.DateTimeField(null=True, blank=True)

    objects = UserManager()

    USERNAME_FIELD = 'phone_number'

    # We do not use password for Django admin generally, it's a 6-digit pin.
    # AbstractBaseUser gives us 'password' field which stores the argon2 hash.

    def __str__(self):
        return self.phone_number

class Device(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='devices')
    device_id = models.CharField(max_length=255)
    name = models.CharField(max_length=255, blank=True)
    platform = models.CharField(max_length=50, blank=True)
    app_version = models.CharField(max_length=50, blank=True)
    is_trusted = models.BooleanField(default=False)

    class Meta:
        unique_together = ('user', 'device_id')

class RefreshTokenFamily(TimestampedModel):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    device = models.ForeignKey(Device, on_delete=models.CASCADE)

class RefreshToken(TimestampedModel):
    jti = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    family = models.ForeignKey(RefreshTokenFamily, on_delete=models.CASCADE, related_name='tokens')
    token_hash = models.CharField(max_length=255, unique=True)
    expires_at = models.DateTimeField()
    is_revoked = models.BooleanField(default=False)

class UserLockout(TimestampedModel):
    phone_number = models.CharField(max_length=15, db_index=True)
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    failed_attempts = models.IntegerField(default=0)
    locked_until = models.DateTimeField(null=True, blank=True)
