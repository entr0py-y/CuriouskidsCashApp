# Skyjumpers Legal & Data Privacy Compliance Report (DPDP Act India 2023)

## 1. Personal Data Inventory
| Data Element | Storage Location | Business Purpose | Retention Period | Encryption |
|---|---|---|---|---|
| Phone Number | PostgreSQL `accounts_user` | Primary Identity / Auth | Account Lifetime | Argon2id Hashed PIN, Encrypted DB |
| Full Name, Email, City | PostgreSQL `profiles_profile` | Profile & Invoicing | Account Lifetime | DB Encryption at Rest |
| Date of Birth | PostgreSQL `profiles_profile` | Age & Minor Verification | Account Lifetime | DB Encryption at Rest |
| Guardian Name & Phone | PostgreSQL `profiles_profile` | Minor Parental Consent | Minor Account Lifetime | DB Encryption at Rest |
| Digital Waivers & Signatures | PostgreSQL `bookings_signedwaiver` | Legal Liability Waiver | 7 Years (Statutory) | Encrypted S3 Object Storage |
| Ledger Transactions | PostgreSQL `wallets_ledgerentry` | Financial Double-Entry Record | 10 Years (Tax Law) | Immutable, DB Encryption |

---

## 2. Minors Protection & Parental Consent Rules
- **Age Rules:** Users under 18 years old are flagged as `is_minor = True`.
- **Guardian Enrolment:** Minor accounts strictly require guardian full name, phone number, relationship, and explicit parental consent confirmation.
- **Top-up Policy:** Self-topups by minors are restricted (`MINORS_CAN_TOPUP = False`).

---

## 3. Data Subject Rights (Export & Erasure)
- **Data Export (GDPR / DPDP Right to Access):** Endpoint `GET /api/v1/me/data-export` exports complete user account, profile, family member, and consent history.
- **Account Deletion (Right to Erasure):** Endpoint `DELETE /api/v1/me/account` verifies wallet balance is zero before scheduling account deletion.
