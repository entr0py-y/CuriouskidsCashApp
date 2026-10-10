# Skyjumpers Backend Architecture

## Overview
The Skyjumpers backend is a monolithic Django application built with Django REST Framework, providing APIs for the native Android customer application and dedicated hardware APIs for cash kiosks and game machines.

## Core Components
- **Django Application**: Exposes versioned APIs (`/api/v1/`) and handles core business logic.
- **PostgreSQL Database**: Relational datastore. Contains the financial ledger which acts as the definitive source of truth for all wallet balances.
- **Redis**: Used for caching, rate limiting (DRF throttling), and as a message broker for Celery.
- **Celery / Celery Beat**: Handles asynchronous background tasks and scheduled jobs (e.g., Outbox pattern processing, report generation).

## Security & Concurrency
- Authentication is handled via secure tokens (to be implemented in B2).
- Hardware APIs require per-machine credentials and replay protection.
- Financial operations use pessimistic row locking (`select_for_update()`) to prevent race conditions and guarantee ACID properties.

## Directory Structure
- `/core/`: Base Django settings, URLs, and shared cross-cutting abstractions.
- `/api/v1/`: Versioned API routing.
- `/docs/`: Architectural Decision Records (ADRs) and documentation.
