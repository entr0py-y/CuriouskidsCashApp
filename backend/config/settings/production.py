import os
from .base import *

DEBUG = False
ALLOWED_HOSTS = env.list('DJANGO_ALLOWED_HOSTS', default=['skyjumpers.in', 'api.skyjumpers.in', 'staging.skyjumpers.in'])

# HTTPS & Security Headers
SECURE_SSL_REDIRECT = env.bool('SECURE_SSL_REDIRECT', default=True)
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_HSTS_SECONDS = 31536000 # 1 year
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True
SECURE_CONTENT_TYPE_NOSNIFF = True
SECURE_BROWSER_XSS_FILTER = True
X_FRAME_OPTIONS = 'DENY'

# CORS Locking
CORS_ALLOWED_ORIGINS = env.list('CORS_ALLOWED_ORIGINS', default=[
    'https://skyjumpers.in',
    'https://app.skyjumpers.in',
    'https://admin.skyjumpers.in'
])
CORS_ALLOW_CREDENTIALS = True

# Request Limits
DATA_UPLOAD_MAX_MEMORY_SIZE = 5242880 # 5 MB max request body
FILE_UPLOAD_MAX_MEMORY_SIZE = 5242880

# Database SSL Connection
DATABASES['default']['OPTIONS'] = {
    'sslmode': env('DB_SSLMODE', default='require'),
}

# Production Logging with PII Scrubbing
LOGGING['handlers']['console']['level'] = 'INFO'
LOGGING['root']['level'] = 'INFO'
