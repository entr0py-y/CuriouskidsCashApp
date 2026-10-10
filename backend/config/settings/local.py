from .base import *

# For local development we enforce DEBUG=True so that developers see stack traces.
DEBUG = True

# Optionally over-ride CACHES to dummy if you don't want Redis local dependency,
# but B1 specifies using Redis local via compose.
