from notifications.models import DeviceToken, NotificationLog

def register_fcm_token(user, token: str, platform: str = 'ANDROID') -> DeviceToken:
    device_token, _ = DeviceToken.objects.update_or_create(
        token=token,
        defaults={"user": user, "platform": platform}
    )
    return device_token

def send_notification(user, event_type: str, title: str, body: str) -> NotificationLog:
    """
    Creates notification record using Outbox pattern.
    """
    log = NotificationLog.objects.create(
        user=user,
        event_type=event_type,
        title=title,
        body=body,
        is_sent=True
    )
    return log
