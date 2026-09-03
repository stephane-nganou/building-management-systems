# Building Management System

Manage apartment buildings: units, tenants, running costs, rent and cold water
invoices as PDF, and a profit and loss statement for your tax declaration.
Owners can delegate fine grained access to assistants.

## Run it

```bash
# Mac
scripts/start-mac.sh      scripts/stop-mac.sh

# Linux
scripts/start-linux.sh    scripts/stop-linux.sh

# Windows
scripts/start-windows.ps1 scripts/stop-windows.ps1
```

Pass `--wipe` (`-Wipe` on Windows) to the stop script to drop the database volume too.

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| API docs | http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:8081 (admin / admin) |

Demo sign in: `owner` / `owner`, or `assistant` / `assistant`. These live in
`docker/keycloak/realm-bms.json` and exist for local development only.

Keycloak takes about a minute on first start while it imports the realm.

## Stack

| Part | Choice |
|---|---|
| Backend | Spring Boot 4.1.1, Java 21, Maven |
| Frontend | Angular 22, standalone and zoneless, signals |
| Database | Postgres 18, persisted in a Docker volume |
| Migrations | Flyway |
| Identity | Keycloak 26.7.3, backend as an OAuth2 resource server |
| Invoice PDF | Thymeleaf template rendered by openhtmltopdf |

**Why Flyway over Liquibase:** migrations stay plain, versioned Postgres SQL that
reads and reviews like the schema it produces. Liquibase's changelog abstraction
buys database portability this project does not need.

**Java 21, not 25:** Boot 4.1 supports 17 through 26. Java 21 is the current LTS
that builds on the machines here; moving to 25 is a one line change to
`java.version` in `backend/pom.xml` plus the base image tags in the Dockerfiles.

## Develop

```bash
# Backend: tests run against a real Postgres via Testcontainers, so Docker must be running.
cd backend && mvn test
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm start
cd frontend && npm test
```

The frontend reads its API and Keycloak URLs at runtime from
`frontend/public/config.js`. The Docker image rewrites that file from the
`API_URL` and `KEYCLOAK_URL` build arguments.

## How access works

Every record belongs to an owner. A user always sees their own data, and an
assistant sees an owner's data only where that owner granted the matching
permission, such as `EXPENSE_READ` or `INVOICE_WRITE`. Owners manage their
assistants under **Assistants**; an assistant must sign in once before they can
be added, which is what creates their local record.

## Layout

```
backend/    Spring Boot service, one package per feature
frontend/   Angular app, one lazy loaded route per feature
docker/     Keycloak realm export and Postgres bootstrap
scripts/    start and stop per platform
docs/       implementation status
```

See [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) for what is
built and what is still open.
