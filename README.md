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

New landlords sign up at http://localhost:4200/register, reachable from
**Register here** on the sign in page. Demo sign in:
`owner` / `owner`, or `assistant` / `assistant`. These live in
`docker/keycloak/realm-bms.json` and exist for local development only.

Keycloak takes about a minute on first start while it imports the realm. The
import runs only when the realm does not exist yet, so an existing local stack
needs `--wipe` on stop to pick up realm changes.

## Stack

| Part | Choice |
|---|---|
| Backend | Spring Boot 4.1.1, Java 25, Maven |
| Frontend | Angular 22, standalone and zoneless, signals |
| Database | Postgres 18, persisted in a Docker volume |
| Migrations | Flyway |
| Identity | Keycloak 26.7.3, backend as an OAuth2 resource server |
| Invoice PDF | Thymeleaf template rendered by openhtmltopdf |
| End to end tests | Playwright, driving the real stack |

**Why Flyway over Liquibase:** migrations stay plain, versioned Postgres SQL that
reads and reviews like the schema it produces. Liquibase's changelog abstraction
buys database portability this project does not need.

**Java 25, not 26:** Boot 4.1 supports 17 through 26. Java 25 is the current LTS;
26 is a six month feature release. A local JDK 26 builds this fine, because
`java.version` makes Maven compile to release 25, and the containers run a 25
runtime. The version lives in `java.version` in `backend/pom.xml` plus the base
image tags in the Dockerfiles.

## Develop

```bash
# Backend: tests run against a real Postgres via Testcontainers, so Docker must be running.
cd backend && mvn test
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm start
cd frontend && npm test

# End to end: brings the whole stack up on its own compose project, runs the
# suite against it, then tears it down. Stop the development stack first, since
# both use the same ports.
scripts/e2e.sh          # mac, linux
scripts/e2e.ps1         # windows
```

The end to end suite runs before every push, through a hook kept in the
repository. Install it once per clone:

```bash
scripts/install-hooks.sh    # or scripts/install-hooks.ps1
```

A hook can be skipped with `git push --no-verify`, so the same suite runs again
on every pull request in GitHub Actions. That is the gate that always holds.

The frontend reads its API and Keycloak URLs at runtime from
`frontend/public/config.js`. The Docker image rewrites that file from the
`API_URL` and `KEYCLOAK_URL` build arguments.

## How access works

Every record belongs to an owner. A user always sees their own data, and an
assistant sees an owner's data only where that owner granted the matching
permission, such as `EXPENSE_READ` or `INVOICE_WRITE`.

Landlords register themselves and get the `owner` realm role. Assistants never
sign up: an owner creates them under **Assistants**, and the app returns a
temporary password once, to hand over. Keycloak makes them choose their own at
first sign in. Both accounts are created through the Keycloak admin API by the
`bms-backend` service account client.

In the browser an assistant is shown only the screens their permissions cover.
The route guards are `canMatch`, so a screen they may not use is never matched
and its code is never downloaded.

## Layout

```
backend/    Spring Boot service, one package per feature
frontend/   Angular app, one lazy loaded route per feature
  e2e/      Playwright specs, run against the whole stack
docker/     Keycloak realm export, login theme and Postgres bootstrap
scripts/    start, stop, end to end and hook installation
.githooks/  pre-push, so a branch is never pushed broken
docs/       implementation status
```

See [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) for what is
built and what is still open.
