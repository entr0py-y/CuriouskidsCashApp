from .base import *

# Test environment MUST have DEBUG = False to mimic production.
DEBUG = False
SECRET_KEY = 'test-secret-key'

# Faster password hashing for tests
PASSWORD_HASHERS = [
    'django.contrib.auth.hashers.MD5PasswordHasher',
]

# Disable celery execution in tests by executing them synchronously
CELERY_TASK_ALWAYS_EAGER = True
CELERY_TASK_EAGER_PROPAGATES = True
