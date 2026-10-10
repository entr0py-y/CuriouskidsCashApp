import json
import logging
import re

class JSONFormatter(logging.Formatter):
    """
    Structured JSON formatter that redacts sensitive PII and financial info.
    """
    # Patterns to redact: Phone numbers, basic card numbers, secrets
    SENSITIVE_PATTERNS = [
        (re.compile(r'\b\d{10}\b'), '[REDACTED_PHONE]'),
        (re.compile(r'(?i)(secret|token|password|pin)=([^\s&]+)'), r'\1=[REDACTED]'),
    ]

    def format(self, record):
        log_record = {
            "timestamp": self.formatTime(record, self.datefmt),
            "level": record.levelname,
            "logger": record.name,
            "message": self._redact(record.getMessage()),
        }

        if record.exc_info:
            log_record['exception'] = self.formatException(record.exc_info)

        return json.dumps(log_record)

    def _redact(self, message: str) -> str:
        for pattern, replacement in self.SENSITIVE_PATTERNS:
            message = pattern.sub(replacement, message)
        return message
