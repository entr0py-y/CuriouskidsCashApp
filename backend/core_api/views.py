from django.http import JsonResponse
from django.db import connection
from django.core.cache import cache
import logging

logger = logging.getLogger(__name__)

def healthz(request):
    """Liveness probe. Returns 200 if the python process is alive."""
    return JsonResponse({"status": "ok"})

def readyz(request):
    """
    Readiness probe. Checks if the application is ready to handle traffic
    by verifying database and redis connectivity.
    """
    try:
        # Check database
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")

        # Check redis (via django cache)
        cache.set('health_check', 'ok', timeout=1)
        if cache.get('health_check') != 'ok':
            raise Exception("Redis cache check failed.")

        return JsonResponse({"status": "ready"})
    except Exception:
        # We explicitly don't log the raw exception string to the client
        # to prevent leaking connection strings/credentials.
        logger.error("Readiness check failed", exc_info=True)
        return JsonResponse({"status": "unready"}, status=503)
