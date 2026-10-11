# Skyjumpers Play Store Readiness & Release Documentation

## 1. Data Safety Form Declarations
| Data Type | Collected | Shared | Purpose | Ephemeral / Stored |
|---|---|---|---|---|
| **Phone Number** | Yes | No | Account Authentication & Identity | Stored (Encrypted) |
| **Name & Email** | Yes | No | Account Management & Invoicing | Stored |
| **Financial Info (Paise)** | Yes | No | Wallet Balance & Topups | Stored (Bank Ledger) |
| **Device ID** | Yes | No | Fraud Prevention & Single Session | Stored |
| **Precise Location** | No | No | N/A | Not Collected |
| **Photos** | Optional | No | Profile Avatar | Stored (S3 Media) |

---

## 2. In-App Account Deletion Flow & Policy URL
- **In-App Flow:** Available under `Profile & Settings` $\rightarrow$ `Delete Account`. Verifies wallet balance is zero before initiating deletion.
- **Privacy Policy URL:** `https://skyjumpers.in/privacy`
- **Account Deletion Request URL:** `https://skyjumpers.in/account-deletion`

---

## 3. Certificate Pinning & Remote Key Rotation Strategy
- **Primary Pin:** `sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=` (Production Certificate).
- **Backup Pin:** `sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=` (Disaster Recovery Key).
- **Remote Kill-Switch Strategy:**
  - If a certificate rotation occurs or a bad pin is deployed, the app checks the version check endpoint (`MIN_APP_VERSION`).
  - If `MIN_APP_VERSION` is updated, the app bypasses strict pinning temporarily or prompts the user with an in-app forced update notification.

---

## 4. Staged Rollout Strategy
1. **Internal Testing (Phase 1):** 100% internal staff & kiosk testing.
2. **Closed Beta (Phase 2):** 5% of park visitors at Delhi Connaught Place branch.
3. **Staged Production Rollout:** `10%` $\rightarrow$ `25%` $\rightarrow$ `50%` $\rightarrow$ `100%` over 7 days.
