# Implementation status

Last updated: 2026-09-03, after BM-1 (foundation of the V1 product).

## Built and working end to end

| Area | State | Notes |
|---|---|---|
| Docker stack | Done | Postgres, Keycloak, backend and frontend; one command to start |
| Start and stop scripts | Done | mac, linux, windows |
| Database schema | Done | Flyway `V1__init.sql`, validated against JPA mappings by every integration test |
| Authentication | Done | Keycloak realm import, backend as OAuth2 resource server, PKCE in the browser |
| Owner and assistant access | Done | Per owner scoping plus 11 delegatable permissions |
| Buildings | Done | Full CRUD, API and UI |
| Apartments | Done | Full CRUD with room layout, rent and status; unique label per building |
| Tenants | Done | Full CRUD, lease dates, deposit, one active tenant per apartment |
| Expenses | Done | Full CRUD, category, reason, optional apartment, filter by building and period |
| Invoices | Done | Rent and cold water, status flow, PDF download |
| Profit and loss | Done | Per building and total, expense breakdown by category |
| Dashboard | Done | Portfolio counts, rent roll, year to date position |
| API documentation | Done | OpenAPI at `/swagger-ui.html` |

Verified against the running stack: sign in, create a building, apartment and
tenant, issue a rent invoice, download its PDF, record an expense and read the
profit and loss.

## Tests

- Backend: 25 tests. Unit tests for invoice totals, rounding and status rules;
  integration tests on a real Postgres 18 via Testcontainers covering the API,
  owner isolation, assistant permissions, PDF rendering and the report maths.
- Frontend: 6 unit tests for the formatting pipes.

## Deliberate decisions

- **Income counts `SENT` and `PAID` invoices**, by issue date. Drafts and
  cancelled invoices are excluded. Report totals are aggregated in memory so
  they always match the rounded per line amounts printed on the invoice.
- **Invoice numbers come from a Postgres sequence** (`INV-<year>-<6 digits>`),
  which stays unique under concurrent creation. A count based scheme would race.
- **A user record is created by a servlet filter**, before any request scoped
  transaction opens. Creating it lazily inside a service silently did nothing
  when the caller's outermost transaction was read only, because Hibernate does
  not flush there.
- **Keycloak's `basic` client scope is required.** Without it the access token
  carries no `sub` claim, which is the only stable user identifier.

## Not built yet

- OAuth 2 beyond the Keycloak resource server setup, as the ticket scopes it later.
- Emailing invoices to tenants; today they are downloaded as PDF.
- Cold water meter readings; a cold water invoice takes its lines directly.
- Attachments or receipts on expenses.
- Pagination. Every list returns all rows, which is fine at a landlord's scale
  but should be revisited before large portfolios.
- Frontend component and end to end tests; only the pipes are covered.
- CI pipeline.

## Known rough edges

- The frontend has no route guard beyond Keycloak's `login-required`; an
  assistant sees every nav entry and gets an empty list where they lack
  permission, rather than the entry being hidden.
- `frontend/public/config.js` is baked at image build, so changing the API or
  Keycloak URL needs an image rebuild rather than a container restart.
