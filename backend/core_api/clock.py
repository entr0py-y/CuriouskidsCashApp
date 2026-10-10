from django.utils import timezone
from datetime import datetime

class Clock:
    """
    Injectable clock abstraction.
    Use Clock.now() instead of timezone.now() in business logic so time can be mocked in tests.
    """
    _mock_time: datetime | None = None

    @classmethod
    def now(cls) -> datetime:
        if cls._mock_time:
            return cls._mock_time
        return timezone.now()

    @classmethod
    def freeze(cls, mock_time: datetime):
        cls._mock_time = mock_time

    @classmethod
    def unfreeze(cls):
        cls._mock_time = None
