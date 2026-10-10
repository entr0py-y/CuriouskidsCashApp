from .base import *

DEBUG = False
# In production, the SECRET_KEY will fail if not provided in the environment.
SECRET_KEY = env('DJANGO_SECRET_KEY')

# In a real environment we would configure logging to ship to Datadog/CloudWatch
# and ensure SECURE_SSL_REDIRECT = True, etc.
