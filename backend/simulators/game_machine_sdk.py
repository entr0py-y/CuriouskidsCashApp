import hmac
import hashlib
import json
import time
import uuid
import urllib.request
import urllib.error

class GameMachineSDK:
    """
    Reference Client SDK for Skyjumpers Arcade Machine Controllers.
    Handles HMAC-SHA256 signing, request formatting, and automatic retry.
    Supports both real HTTP requests (production/staging) and DRF APIClient (in-memory test mode).
    """
    def __init__(self, machine_id: str, secret: str, base_url: str = "http://localhost:8000", client = None):
        self.machine_id = machine_id
        self.secret = secret
        self.base_url = base_url.rstrip("/")
        self.client = client

    def _sign_and_request(self, method: str, path: str, payload_dict: dict) -> dict:
        body_bytes = json.dumps(payload_dict, separators=(',', ':')).encode('utf-8') if payload_dict else b''
        body_sha256 = hashlib.sha256(body_bytes).hexdigest()

        ts = str(int(time.time()))
        nonce = str(uuid.uuid4())
        canonical = f"{method.upper()}\n{path}\n{ts}\n{nonce}\n{body_sha256}"
        signature = hmac.new(self.secret.encode('utf-8'), canonical.encode('utf-8'), hashlib.sha256).hexdigest()

        headers = {
            "HTTP_X_MACHINE_ID": self.machine_id,
            "HTTP_X_TIMESTAMP": ts,
            "HTTP_X_NONCE": nonce,
            "HTTP_X_SIGNATURE": signature,
        }

        if self.client:
            # In-memory DRF test client
            resp = self.client.post(path, data=payload_dict, format='json', **headers)
            try:
                return resp.json()
            except Exception:
                return {"error": {"code": "HTTP_ERROR", "message": resp.content.decode()}}

        # Real HTTP connection
        url = f"{self.base_url}{path}"
        http_headers = {
            "Content-Type": "application/json",
            "X-Machine-ID": self.machine_id,
            "X-Timestamp": ts,
            "X-Nonce": nonce,
            "X-Signature": signature
        }
        req = urllib.request.Request(url, data=body_bytes, headers=http_headers, method=method.upper())
        try:
            with urllib.request.urlopen(req, timeout=5) as response:
                return json.loads(response.read().decode('utf-8'))
        except urllib.error.HTTPError as e:
            error_content = e.read().decode('utf-8')
            try:
                return json.loads(error_content)
            except Exception:
                return {"error": {"code": "HTTP_ERROR", "message": error_content}}
        except Exception as e:
            return {"error": {"code": "NETWORK_ERROR", "message": str(e)}}

    def identify(self, qr_token: str) -> dict:
        return self._sign_and_request("POST", "/api/machine/v1/identify", {"qr_token": qr_token})

    def charge(self, qr_token: str, game_code: str, idempotency_key: str, max_retries: int = 3) -> dict:
        payload = {
            "qr_token": qr_token,
            "game_code": game_code,
            "idempotency_key": idempotency_key
        }
        for attempt in range(max_retries):
            res = self._sign_and_request("POST", "/api/machine/v1/charge", payload)
            if res.get("approved") is True or "error" in res:
                return res
            time.sleep(0.1)
        return res

    def refund(self, transaction_id: str, reason: str) -> dict:
        return self._sign_and_request("POST", f"/api/machine/v1/charge/{transaction_id}/refund", {"reason": reason})

    def heartbeat(self, status: str = "ONLINE", software_version: str = "1.0.0") -> dict:
        return self._sign_and_request("POST", "/api/machine/v1/heartbeat", {
            "status": status,
            "software_version": software_version
        })
