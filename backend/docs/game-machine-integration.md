# Skyjumpers Game Machine Integration Contract

## 1. Overview
This document specifies the exact contract, state machine, request signing algorithm, and failure handling rules for arcade game machines, turnstile gates, and VR cabinets connecting to the Skyjumpers core platform via `/api/machine/v1/`.

---

## 2. Machine State Machine
Game machines MUST strictly adhere to the following 5-state lifecycle:

```text
       +--------------+
       |     IDLE     |<--------------------+
       +--------------+                     |
              | (QR Scanned)                |
              v                             |
       +--------------+                     |
       |   SCANNED    |                     |
       +--------------+                     |
              | (User Confirms / Auto-Play) |
              v                             |
       +--------------+                     |
       |   CHARGING   |                     |
       +--------------+                     |
         /          \                       |
(Approved)        (Declined)                |
       v              v                     |
+--------------+  +-------------------+     |
|   PLAYING    |  | DECLINED (Screen) |-----+
+--------------+  +-------------------+
       | (Hardware Fault)
       v
+--------------+
|    FAULT     |---> (POST /refund) ---> IDLE
+--------------+
```

### State Descriptions
1. **`IDLE`**: Machine is waiting for a customer. Screen shows "Scan App QR to Play".
2. **`SCANNED`**: Scanner detected `SKYJ1:<token>`. Machine calls `POST /identify`. Shows "Hello <Name>, Balance ₹X".
3. **`CHARGING`**: Machine calls `POST /charge`. Button/start relay disabled during API call.
4. **`PLAYING`**: Backend returned `approved: true`. Machine triggers game start pulse / relay for configured duration.
5. **`FAULT`**: Game failed to activate (e.g. coin drop relay jam, motor failure). Machine calls `POST /charge/<transaction_id>/refund` to issue an immediate double-entry reversal, then returns to `IDLE`.

---

## 3. HMAC-SHA256 Request Signing
All machine requests (except `/activate`) require HMAC-SHA256 signature headers.

### Required Headers
- `X-Machine-ID`: Machine UUID.
- `X-Timestamp`: Unix epoch timestamp in seconds.
- `X-Nonce`: Random UUID or string (single-use within 120s window).
- `X-Signature`: Hex-encoded HMAC-SHA256 digest over the canonical string.

### Canonical String Format
```text
<HTTP_METHOD>\n
<HTTP_PATH>\n
<TIMESTAMP>\n
<NONCE>\n
<BODY_SHA256>
```
*Note:* `BODY_SHA256` is the hex digest of `SHA-256(request_body)`. For empty request bodies, use `SHA-256("")` = `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.

### Worked Example & Test Vectors
- **Machine ID:** `11111111-2222-3333-4444-555555555555`
- **Secret:** `secret_key_123`
- **Method:** `POST`
- **Path:** `/api/machine/v1/charge`
- **Timestamp:** `1700000000`
- **Nonce:** `nonce-game-001`
- **Body:** `{"qr_token":"SKYJ1:token_xyz","game_code":"TEKKEN","idempotency_key":"IDEM_1"}`
- **Body SHA-256:** `0a97c36a461b61971166a0d014c2b988f0bc2e5352618991a0c776fb0d283626`

**Canonical String:**
```text
POST
/api/machine/v1/charge
1700000000
nonce-game-001
0a97c36a461b61971166a0d014c2b988f0bc2e5352618991a0c776fb0d283626
```

**Signature (HMAC-SHA256 Hex):** `cfbd1a539bc7a60216503c8c2045e0fb5250428fa6b0e8b15d2a71d7946927d3`

---

## 4. Error & Decline Codes Display Mapping

| Backend Error Code | Cause | Display Text on Game Machine Screen |
|---|---|---|
| `INSUFFICIENT_FUNDS` | Wallet balance < game price | "Insufficient Balance. Please recharge app." |
| `TOKEN_EXPIRED` | QR token > 45s old | "QR Expired. Please refresh app." |
| `TOKEN_REUSED` | QR token `jti` already spent | "QR Already Used. Tap eye for new QR." |
| `USER_SUSPENDED` | Account deleted / suspended | "Account Restricted. Contact counter." |
| `MACHINE_DISABLED` | Machine disabled in admin | "Out of Service" |
| `CLOCK_SKEW_REJECTED` | Machine RTC clock off by > 60s | "Hardware Time Error" |

---

## 5. QR Code Format & Hardware Scanner Configuration
- **Format String:** `SKYJ1:<ES256_JWT_TOKEN>`
- **Scanner Type:** 2D QR Image Scanner (USB HID Keyboard or RS232 Serial).
- **Serial Settings:** `115200 Baud, 8 Data Bits, No Parity, 1 Stop Bit (8N1)`.
- **Prefix / Suffix:** Configured to emit suffix `CR` or `LF` (`\r` or `\n`) after barcode string to trigger scanner input buffer flush.

---

## 6. Offline & Retry Rules
1. **Fail Closed:** Game machines **MUST NEVER** start game play without an explicit `approved: true` HTTP response from the backend.
2. **Network Timeouts during Charge:** If a `POST /charge` request times out or loses connection, the machine MUST retry using the **EXACT SAME `idempotency_key`**. The server guarantees no double-charging.
3. **Heartbeat:** Every 30 seconds, the machine MUST send `POST /heartbeat`.
