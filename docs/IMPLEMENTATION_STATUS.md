# Implementation status

Last updated: 2026-09-04, after BM-3 (registration, permission aware navigation
and the end to end suite).

## Built and working end to end

| Area | State | Notes |
|---|---|---|
| Docker stack | Done | Postgres, Keycloak, backend and frontend; one command to start |
| Start and stop scripts | Done | mac, linux, windows |
| Database schema | Done | Flyway `V1__init.sql`, validated against JPA mappings by every integration test |
| Authentication | Done | Keycloak realm import, backend as OAuth2 resource server, PKCE in the browser |
| Registration | Done | Public `/register` page and `POST /api/auth/register`; new users get the `owner` realm role |
| Sign in page | Done | Keycloak login theme in `docker/keycloak/themes/bms`, linking to our registration page |
| Assistant accounts | Done | The owner creates them; a temporary password is returned once |
| Permission aware UI | Done | `canMatch` guards and a filtered sidebar; a denied screen is never downloaded |
| Owner and assistant access | Done | Per owner scoping plus 11 delegatable permissions |
| Buildings | Done | Full CRUD, API and UI |
| Apartments | Done | Full CRUD with room layout, rent and status; unique label per building |
| Tenants | Done | Full CRUD, lease dates, deposit, one active tenant per apartment |
| Expenses | Done | Full CRUD, category, reason, optional apartment, filter by building and period |
| Invoices | Done | Rent and cold water, status flow, PDF download |
| Profit and loss | Done | Per building and total, expense breakdown by category |
| Dashboard | Done | Portfolio counts, rent roll, year to date position |
| API documentation | Done | OpenAPI at `/swagger-ui.html` |
| End to end tests | Done | Playwright against the real stack, run before every push and on every pull request |
| CI pipeline | Done | GitHub Actions: backend, frontend and end to end |

Verified against the running stack: sign in, create a building, apartment and
tenant, issue a rent invoice, download its PDF, record an expense and read the
profit and loss. For BM-3, against a throwaway stack: register from the browser,
sign in with the new account and see the `owner` role in its token, create an
assistant and confirm the account carries the `assistant` role and an
`UPDATE_PASSWORD` action, and confirm the password is returned once and never
again on a later read.

## Tests

- Backend: 31 tests. Unit tests for invoice totals, rounding and status rules;
  integration tests on a real Postgres 18 via Testcontainers covering the API,
  owner isolation, assistant permissions, registration, PDF rendering and the
  report maths. The Keycloak admin client is mocked there.
- Frontend: 15 unit tests, for the formatting pipes and for the session and
  route guard logic that decides which screens exist.
- End to end: 5 Playwright specs against the running stack, covering the link
  from the sign in page to registration, signing up and landing on a full
  portfolio, the duplicate email refusal, an owner creating an assistant who
  then has to choose a password and sees only their one granted screen, and
  adding a building with an apartment in it. `scripts/e2e` starts the stack on
  its own compose project, waits for every part, runs them and tears it down.

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
- **Accounts are created through the Keycloak admin REST API** with a plain
  `RestClient`, driven by the `bms-backend` service account client. The official
  admin client would pull a whole JAX-RS stack in for four calls. Note that Boot
  4 does not auto configure a `RestClient.Builder` bean here, so the client is
  built directly.
- **Registration mirrors the account locally straight away** rather than waiting
  for the provisioning filter, so an owner can create an assistant and grant
  them work before that assistant has ever signed in.
- **Route guards are `canMatch`, not `canActivate`**, because only `canMatch`
  stops the router from matching the route at all, which is what keeps the lazy
  chunk from being fetched. A redirecting route cannot carry a guard either, so
  the empty path is a small component that navigates on, rather than a redirect.
- **The profile is loaded by the guards, not an app initializer.** Angular
  starts every initializer at once without waiting for the one before, so an
  initializer here ran while Keycloak was still deciding whether there was a
  session, read `authenticated` as false and bounced the browser between the app
  and the login page for ever. Guards run after bootstrap, when Keycloak is
  ready. The end to end suite is what caught this.
- **A failed profile request is not remembered**, so the next guard tries again
  rather than stranding the user on the empty page because the backend was a
  moment slower to start than the browser.
- **The sign in page is themed rather than replaced.** Keycloak's own
  registration stays off, because signing up has to assign the owner role, which
  only our backend does. The theme copies one 56 line template from
  `keycloak.v2` and changes its footer to point at our page.
- **An owner is anyone whose token does not say `assistant`.** Registration
  assigns the `owner` role, but treating a missing role as an owner keeps
  accounts made before roles existed working.

## Not built yet

- OAuth 2 beyond the Keycloak resource server setup, as the ticket scopes it later.
- Email verification and password reset by email; both are Keycloak features
  that need an SMTP server configured.
- Emailing invoices to tenants; today they are downloaded as PDF.
- Cold water meter readings; a cold water invoice takes its lines directly.
- Attachments or receipts on expenses.
- Pagination. Every list returns all rows, which is fine at a landlord's scale
  but should be revisited before large portfolios.
- Component level frontend tests. The pipes, session and guards have unit tests
  and the main journeys have end to end ones, but individual screens do not.
- End to end coverage of invoices and the profit and loss report.

## Known rough edges

- Write actions are still shown inside a screen an assistant may only read; the
  backend refuses them, but the buttons are there.
- The end to end stack uses the same ports as the development one, because the
  realm's redirect URIs name them, so the two cannot run at once.
- `docker/keycloak/themes/bms` hardcodes the registration URL, for the same
  reason `frontend/public/config.js` hardcodes the API host.
- The `bms-backend` client secret is a literal in the realm export, matched by a
  default in `application.yml`. Fine locally; a deployment has to set
  `BMS_KEYCLOAK_ADMIN_CLIENT_SECRET` and a realm to match.
- `frontend/public/config.js` is baked at image build, so changing the API or
  Keycloak URL needs an image rebuild rather than a container restart.
