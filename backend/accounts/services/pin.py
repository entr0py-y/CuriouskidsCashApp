import re

def is_weak_pin(pin: str, phone_number: str) -> bool:
    """
    Rejects weak PINs: all same digits, simple sequences, common patterns,
    and the last 6 digits of the phone number.
    """
    if not pin or len(pin) != 6 or not pin.isdigit():
        return True

    # All same digits (e.g., 111111)
    if len(set(pin)) == 1:
        return True

    # Simple sequences
    if pin in ('123456', '654321', '012345', '543210'):
        return True

    # Last 6 digits of phone
    clean_phone = re.sub(r'\D', '', phone_number)
    if len(clean_phone) >= 6 and pin == clean_phone[-6:]:
        return True

    return False
