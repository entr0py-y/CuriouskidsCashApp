# Skyjumpers Ledger & Wallet

## Design Principles
The Skyjumpers backend implements a strict Double-Entry accounting ledger. 
- **Integer Paise:** Money is exclusively represented in integer `paise`. 100 paise = 1 INR. Floating point math is prohibited to prevent rounding errors.
- **Append-Only:** Ledger records (`LedgerTransaction` and `LedgerEntry`) are strictly immutable. They cannot be updated or deleted. Corrections are handled via explicit `REVERSAL` transactions.
- **Invariants:** 
  1. `SUM(DEBIT) == SUM(CREDIT)` for every single transaction. This is enforced at the DB level by a PostgreSQL deferred constraint trigger (`ensure_transaction_balance`) attached to the `wallets_ledgerentry` table.
  2. `Wallet.main_balance >= 0` and `Wallet.bonus_balance >= 0`. Enforced by DB-level `CHECK` constraints.

## Ledger Accounts
| Type | Purpose | Owner |
|---|---|---|
| `USER_WALLET_MAIN` | Holds real INR balance top-ups. | Customer |
| `USER_WALLET_BONUS` | Holds promotional bonus value. | Customer |
| `GATEWAY_CLEARING` | System account tracking money entering via Payment Gateways. | System (None) |
| `CASH_MACHINE_CLEARING` | System account tracking physical cash inserted at Kiosks. | System (None) |
| `REVENUE` | System account where spent customer funds ultimately rest. | System (None) |

## Transaction Double-Entry Maps
### Top-up via Gateway
| Account Type | Direction | Amount |
|---|---|---|
| `GATEWAY_CLEARING` | DEBIT | +X |
| `USER_WALLET_MAIN` | CREDIT | +X |

### Spend at Game Machine (using only Main Balance)
| Account Type | Direction | Amount |
|---|---|---|
| `USER_WALLET_MAIN` | DEBIT | +Y |
| `REVENUE` | CREDIT | +Y |

### Spend at Game Machine (using Bonus Balance)
The backend `debit_spend` service drains `USER_WALLET_BONUS` before touching `USER_WALLET_MAIN`.
If a game costs 100, and the user has 40 Bonus and 100 Main:
| Account Type | Direction | Amount |
|---|---|---|
| `USER_WALLET_BONUS` | DEBIT | +40 |
| `REVENUE` | CREDIT | +40 |
| `USER_WALLET_MAIN` | DEBIT | +60 |
| `REVENUE` | CREDIT | +60 |

## Idempotency & Concurrency
- `idempotency_key` has a `UNIQUE` constraint. 
- Using `select_for_update()` on the `Wallet` model ensures that parallel requests (e.g., spending at 2 machines simultaneously) are queued at the database level and executed sequentially. 

## Reconciliation
The background job `reconcile_wallets()` runs continuously via Celery Beat. It recalculates every user's cached `Wallet.main_balance` by independently summing all underlying `LedgerEntry` credits and debits, raising an alarm if the cached value ever drifts from the true ledger.
