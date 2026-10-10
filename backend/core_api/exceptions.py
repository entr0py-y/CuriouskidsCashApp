from rest_framework.views import exception_handler
from rest_framework.exceptions import ValidationError

def custom_exception_handler(exc, context):
    """
    Standardizes all DRF error responses to the Skyjumpers format:
    { "error": { "code": "...", "message": "...", "details": {}, "request_id": "..." } }
    """
    response = exception_handler(exc, context)

    if response is not None:
        request_id = getattr(context['request'], 'id', 'unknown')

        # Determine error code
        code = getattr(exc, 'default_code', 'error')
        if hasattr(exc, 'get_codes'):
            codes = exc.get_codes()
            if isinstance(codes, dict):
                code = 'validation_error'
            elif isinstance(codes, list) and len(codes) > 0:
                code = codes[0]

        # Produce clean human-readable message for validation & throttled errors
        msg = str(exc)
        detail = getattr(exc, 'detail', None)
        if isinstance(detail, dict):
            first_key = next(iter(detail))
            first_val = detail[first_key]
            if isinstance(first_val, list) and len(first_val) > 0:
                first_item = first_val[0]
                msg = getattr(first_item, 'string', str(first_item))
            elif isinstance(first_val, str):
                msg = first_val
        elif isinstance(detail, list) and len(detail) > 0:
            first_item = detail[0]
            msg = getattr(first_item, 'string', str(first_item))

        formatted_error = {
            "error": {
                "code": str(code).upper(),
                "message": msg,
                "details": response.data if isinstance(response.data, dict) else {},
                "request_id": request_id
            }
        }
        response.data = formatted_error

    return response
