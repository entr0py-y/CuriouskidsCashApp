# Skyjumpers Backend

Backend services for the Skyjumpers amusement park platform.

## Architecture

- **Framework**: Django LTS & Django REST Framework
- **Database**: PostgreSQL (Source of truth for all balances)
- **Cache/Queue**: Redis
- **Background Tasks**: Celery
- **Monorepo Structure**: This backend co-exists with the native Android `app` inside the same project repository.

## Local Development

### Prerequisites
- Python 3.11+
- Docker & Docker Compose
- Git

### Setup
1. Copy the environment variables template:
   ```bash
   cp .env.example .env
   ```
2. Start the infrastructure (PostgreSQL, Redis) and the Django server using Docker Compose:
   ```bash
   docker-compose up --build
   ```

### Running Tests
The test suite utilizes `pytest`. Ensure the database and redis are running.
```bash
docker-compose run web pytest
```

### Health Endpoints
- `/api/v1/healthz`: Liveness probe.
- `/api/v1/readyz`: Readiness probe (checks DB and Redis).

### Financial Rules (Non-Negotiable)
- All money is represented in integer **paise**. NO floating-point math.
- The backend ledger is the absolute authority.
- All ledger transactions must balance debits and credits and be idempotent.
