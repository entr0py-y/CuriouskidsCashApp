# ADR 001: Technology Stack Decision

## Status
Accepted

## Context
We need a robust, transactional, and rapid-development framework to serve as the backend for the Skyjumpers amusement park platform. The platform handles sensitive financial operations (digital wallets, machine credits), requires reliable background task processing, and serves both customer-facing Android applications and hardware-based kiosks.

## Decision
We will use:
- **Python / Django LTS**: For rapid, secure business logic development.
- **Django REST Framework**: For API construction, serialization, and throttling.
- **PostgreSQL**: As the primary relational database, specifically for its strong ACID guarantees and row-level locking capabilities necessary for the wallet ledger.
- **Redis & Celery**: For handling asynchronous tasks, caching, and rate limiting.
- **Docker Compose**: To ensure parity between local development and production environments.

## Consequences
- **Positive**: High developer velocity, vast ecosystem for background processing, excellent ORM for complex transactional queries.
- **Negative**: Requires strict adherence to performance best practices (N+1 query prevention) and strict environment boundaries since Django uses synchronous execution by default. 
