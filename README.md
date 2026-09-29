# Building Management System

Manage apartment buildings: units, tenants, running costs, rent and cold water
invoices as PDF, and a profit and loss statement for your tax declaration.
Owners can delegate fine grained access to assistants. In English or French.

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

The app opens in French for a French browser and English for anything else;
**EN / FR** in the sidebar changes it and the choice is remembered. The sign in
page, the API's error messages and invoice PDFs follow the same choice.

Keycloak takes about a minute on first start while it imports the realm. The
import runs only when the realm does not exist yet, so a stack that has been
started once will not see later changes to `docker/keycloak/realm-bms.json`.
Apply them to a running Keycloak instead of wiping the database:

```bash
node scripts/sync-realm.mjs
```

It updates the realm settings, adds any missing roles and refreshes the
clients, then checks that the backend can still get a token. Users are left
alone, so nobody loses their account or their buildings.

## Stack

| Part | Choice |
|---|---|
| Backend | Spring Boot 4.1.1, Java 25, Maven |
| Frontend | Angular 22, standalone and zoneless, signals |
| Database | Postgres 18, persisted in a Docker volume |
| Migrations | Flyway |
| Identity | Keycloak 26.7.3, behind the backend: OAuth2 client for the browser, resource server for everyone else |
| Invoice PDF | Thymeleaf template rendered by openhtmltopdf |
| End to end tests | Playwright, driving the real stack |
| Languages | English and French, switched at runtime without a reload |

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

The frontend has nothing to configure. nginx serves the bundle and forwards
`/api` to the backend, so every URL the application uses is relative and the
browser only ever talks to one origin. `BACKEND_ORIGIN` on the frontend
container says where `/api` goes; `ng serve` uses `frontend/proxy.conf.json`
for the same purpose.

## How access works

Every record belongs to an owner. A user always sees their own data, and an
assistant sees an owner's data only where that owner granted the matching
permission, such as `EXPENSE_READ` or `INVOICE_WRITE`.

Landlords register themselves and get the `owner` realm role. Assistants never
sign up: an owner creates them under **Assistants**, and the app returns a
password once, to hand over. The app then makes them choose their own before
showing them anything else. Both accounts are created through the Keycloak admin
API by the `bms-backend` client.

**The Angular app knows one host: its own.** It never names Keycloak, holds no
token and carries no identity library. Signing in is a navigation to
`/api/auth/login/keycloak`, where the backend runs the authorization code flow
and hands back an `HttpOnly` session cookie; the sign in page itself is the only
part of Keycloak anybody sees, and the browser gets there because the backend
redirected it. Writes carry a forgery token, since a cookie alone travels
whether or not the user meant it to.

The same API serves clients that are not this browser. A mobile application does
its own authorization code flow with Keycloak, as native apps should, and sends
`Authorization: Bearer` to exactly these endpoints; the backend resolves a
session and a token to the same user, roles and permissions.

This is a deliberate trade. Keeping tokens out of the page is what the OAuth 2.0
browser-based-apps guidance recommends, because no token JavaScript can read
survives cross site scripting. What it buys is containment rather than
prevention: an injected script can still act as the user while the tab is open,
but it cannot carry a credential away and keep using it.

In the browser an assistant is shown only the screens their permissions cover.
The route guards are `canMatch`, so a screen they may not use is never matched
and its code is never downloaded.

## Layout

```
backend/    Spring Boot service, one package per feature
frontend/   Angular app, one lazy loaded route per feature
  e2e/      Playwright specs, run against the whole stack
docker/     Keycloak realm export, sign in theme and Postgres bootstrap
scripts/    start, stop, end to end, realm sync and hook installation
.githooks/  pre-push, so a branch is never pushed broken
docs/       implementation status and architecture diagrams
```

See [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) for what is
built and what is still open, and
[docs/ARCHITECTURE_DIAGRAMS.md](docs/ARCHITECTURE_DIAGRAMS.md) for the
containers, the domain model, the schema and the main flows drawn out.
