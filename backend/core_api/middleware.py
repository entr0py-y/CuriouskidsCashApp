import uuid

class RequestIDMiddleware:
    """
    Middleware that assigns a unique request ID to every incoming request.
    Useful for distributed tracing and correlating logs.
    """
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request_id = request.headers.get('X-Request-ID')
        if not request_id:
            request_id = str(uuid.uuid4())

        request.id = request_id

        response = self.get_response(request)
        response['X-Request-ID'] = request_id
        return response
