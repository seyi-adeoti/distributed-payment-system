# Orders Service

This Spring Boot service runs independently with its own Maven build, Postgres database, and port.

## Run

From the workspace root:

```bash
docker compose up --build orders-service
```

The service will be available at http://localhost:8082/api/orders/health.
