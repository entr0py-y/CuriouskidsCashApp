# Skyjumpers Machine API & QR Specification

## Overview
The Machine API (`/api/machine/v1/`) is a dedicated namespace for hardware devices (game machines, turnstiles, cash kiosks) to interact with the backend. It is completely isolated from the customer API.

---

## 1. Request Signing Scheme (HMAC-SHA256)
Every machine request (except one-time activation) must include four custom security headers.

### Headers Required
- `X-Machine-ID`: Machine UUID.
- `X-Timestamp`: Current Unix epoch seconds.
- `X-Nonce`: Unique per-request UUID or random string.
- `X-Signature`: Hex-encoded HMAC-SHA256 signature.

### Canonical String Format
```
<HTTP_METHOD>\n
<HTTP_PATH>\n
<TIMESTAMP>\n
<NONCE>\n
<BODY_SHA256>
```
*Note:* `BODY_SHA256` is the hex digest of `SHA-256(request_body)`. For empty bodies, use `SHA-256("")` -> `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.

### Worked Example & Test Vectors
- **Shared Secret:** `secret_key_123`
- **Method:** `POST`
- **Path:** `/api/machine/v1/heartbeat`
- **Timestamp:** `1700000000`
- **Nonce:** `nonce-abc-123`
- **Body:** `{"status":"ONLINE"}`
- **Body SHA-256:** `4d89fa326880482a720e3dd82c16eb251f21ebaa92110c7104b2ebff54e4e941`

**Canonical String:**
```text
POST
/api/machine/v1/heartbeat
1700000000
nonce-abc-123
4d89fa326880482a720e3dd82c16eb251f21ebaa92110c7104b2ebff54e4e941
```

**HMAC-SHA256 Hex Digest:** `2986423ca87889e40f6f8517228ec522ec4bf4912959048a129d235889704e9c`

---

## 2. Endpoints Reference
### `POST /api/machine/v1/activate`
One-time provisioning flow.
- **Request:** `{"activation_code": "ACTIVATION_CODE_123"}`
- **Response:** `{"machine_id": "<uuid>", "secret": "<32_byte_hex_secret>"}`

### `POST /api/machine/v1/identify`
Identifies customer at game or kiosk. Returns minimal data.
- **Request:** `{"qr_token": "SKYJ1:..."}`
- **Response:** `{"user_ref": "<uuid>", "display_name": "Firstname", "wallet_total_paise": 10000, "eligible": true, "reason": null}`

### `POST /api/machine/v1/charge`
Charges customer for a game. Server price list is the **only** authority for pricing.
- **Request:** `{"qr_token": "SKYJ1:...", "game_code": "TEKKEN", "idempotency_key": "<uuid>"}`
- **Response:** `{"approved": true, "charged_paise": 5000, "balance_after_paise": 5000, "transaction_id": "<uuid>"}`

### `POST /api/machine/v1/charge/<uuid:transaction_id>/refund`
Refunds a charge in case of hardware error.
- **Request:** `{"reason": "BALL_DISPENSER_JAMMED"}`
- **Response:** `{"refunded": true, "reversal_transaction_id": "<uuid>"}`

### `POST /api/machine/v1/heartbeat`
Pings server status.
- **Request:** `{"status": "ONLINE", "software_version": "2.1.0", "validator_status": {}}`
- **Response:** `{"status": "ACK", "server_time": "2026-10-10T16:50:00Z"}`

---

## 3. Error Codes & Rejection Handling
- `CLOCK_SKEW_REJECTED`: Timestamp differs by > 60s from server clock.
- `NONCE_REUSED`: Nonce was sent within last 120s window.
- `INVALID_SIGNATURE`: HMAC digest mismatch.
- `MACHINE_DISABLED`: Machine disabled by admin.
- `TOKEN_EXPIRED`: QR token > 45s old.
- `TOKEN_REUSED`: QR token `jti` already spent.
- `INSUFFICIENT_FUNDS`: Customer balance < game price.

---

## 4. Offline & Retry Rules for Firmware
- **No Offline Play:** Machines **MUST NOT** approve play or dispense items without explicit server approval.
- **Network Timeouts:** If a `/charge` request times out, the machine MUST retry using the **exact same `idempotency_key`**. The server guarantees no double charge.
