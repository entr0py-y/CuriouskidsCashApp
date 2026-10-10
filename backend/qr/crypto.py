import jwt
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization

# Generate ECDSA P-256 Key pair for Ed25519/ES256 signing
_private_key = ec.generate_private_key(ec.SECP256R1())
_public_key = _private_key.public_key()

KID = "skyjumpers-qr-key-2026-v1"

def get_private_pem() -> bytes:
    return _private_key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption()
    )

def get_public_pem() -> bytes:
    return _public_key.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )

def get_jwks() -> dict:
    # Public JWKS representation
    public_numbers = _public_key.public_numbers()
    import base64

    def int_to_base64url(val: int) -> str:
        b = val.to_bytes((val.bit_length() + 7) // 8, byteorder='big')
        return base64.urlsafe_b64encode(b).decode('utf-8').rstrip('=')

    return {
        "keys": [
            {
                "kty": "EC",
                "crv": "P-256",
                "use": "sig",
                "kid": KID,
                "x": int_to_base64url(public_numbers.x),
                "y": int_to_base64url(public_numbers.y)
            }
        ]
    }
