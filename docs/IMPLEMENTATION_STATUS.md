# Implementation status

Last updated: 2026-09-04, after BM-4 (English and French across the app, the
API, the sign in page and invoice PDFs).

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
| English and French | Done | Every screen, API error, sign in page and invoice PDF |
| API documentation | Done | OpenAPI at `/swagger-ui.html` |
| End to end tests | Done | Playwright against the real stack, run before every push and on every pull request |
| CI pipeline | Done | GitHub Actions: backend, frontend and end to end |

Verified against the running stack: sign in, create a building, apartment and
tenant, issue a rent invoice, download its PDF, record an expense and read the
profit and loss. For BM-3, against a throwaway stack: register from the browser,
sign in with the new account and see the `owner` role in its token, create an
assistant and confirm the account carries the `assistant` role and an
`UPDATE_PASSWORD` action, and confirm the password is returned once and never
again on a later read. For BM-4, against a throwaway stack: switch the whole app
to French and back and watch it hold across a reload, land on a French
registration page from a French browser, read the Keycloak sign in page in both
languages, and download the same invoice as a French and an English PDF.

## Tests

- Backend: 40 tests. Unit tests for invoice totals, rounding and status rules;
  integration tests on a real Postgres 18 via Testcontainers covering the API,
  owner isolation, assistant permissions, registration, PDF rendering and the
  report maths. The language ones read the words back out of a rendered PDF
  with PDFBox, since a template that resolved no messages would still be a
  valid PDF. The Keycloak admin client is mocked there.
- Frontend: 27 unit tests, for the formatting pipes, the translation service and
  its dictionaries, and for the session and route guard logic that decides which
  screens exist.
- End to end: 7 Playwright specs against the running stack, covering the link
  from the sign in page to registration, signing up and landing on a full
  portfolio, the duplicate email refusal, an owner creating an assistant who
  then has to choose a password and sees only their one granted screen, adding a
  building with an apartment in it, switching the app to French and back, and a
  French browser landing on a French registration page. `scripts/e2e` starts the
  stack on its own compose project, waits for every part, runs them and tears it
  down.

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
- **Syncing the realm never overwrites a role.** Keycloak's partial import
  replaces a role by deleting and recreating it, which drops it from every user
  who held it: the demo owner stops being an owner, and worse, an assistant who
  loses their marker is treated as one. Roles are added and never replaced;
  clients, where redirect URIs and secrets actually drift, are replaced.
- **A Keycloak failure is a 502, never a 401.** The admin client's own HTTP
  errors used to escape untouched, so a realm missing the `bms-backend` client
  answered registration with a bare 401 and an empty body. That reads as "you
  are not signed in" on an endpoint that needs no sign in, which is the worst
  possible signpost. The reason is logged; the caller is only told to try later.
- **An owner is anyone whose token does not say `assistant`.** Registration
  assigns the `owner` role, but treating a missing role as an owner keeps
  accounts made before roles existed working.
- **Translation is a runtime lookup, not Angular's build time i18n.** `$localize`
  produces a bundle per language, served under its own path, which needs the web
  server to route and a full rebuild to change a word. A signal held dictionary
  serves both languages from one bundle and switches without a reload, which is
  what a language toggle in the sidebar has to do.
- **The English dictionary is the source of truth for the keys.** `MessageKey` is
  derived from it and French is typed as `Record<MessageKey, string>`, so a key
  missing from French fails the build rather than leaving a blank on screen. A
  unit test covers the half TypeScript cannot see: a key left holding the English
  text.
- **The translation pipes are impure.** A pure pipe caches on its argument, and
  the argument here is the message key, which does not change when the language
  does; the old wording would stay on screen. An impure pipe reads the language
  signal on every run, which both registers the dependency with the view and
  recomputes once it is marked dirty.
- **Enum labels are looked up per family, not humanised from the value.** The old
  `LabelPipe` turned `COLD_WATER` into "Cold water", which only ever works in
  English. Lookups need the family as well as the value, because `MAINTENANCE` is
  an apartment under works and an expense on upkeep, and French has a separate
  word for each.
- **The backend answers in the language of `Accept-Language`.** Exceptions carry
  a message code and its arguments rather than a finished sentence, and the
  handler resolves them against `messages.properties`. The resolver's list of
  languages is closed, so an unknown one falls back to English rather than to
  whatever locale the container happens to run under.
- **French messages use the typographic apostrophe.** Spring runs MessageFormat
  over any message that takes arguments, and there `'` is an escape character
  that silently eats the text around it. `’` is both safe and correct French, so
  it is used throughout rather than doubling quotes in some messages and not
  others.
- **Generated invoice lines keep the language they were written in.** "Rent 1A"
  and "Utilities advance" are stored on the invoice when it is created, exactly
  like a line the user typed, so they are translated once at that moment and
  never again. Only the wording around them follows the download request. Making
  them follow the reader would mean storing a code instead of text, and no line
  the user writes could work that way.
- **The end to end suite pins the browser language.** The app reads
  `navigator.language` on a first visit, so without `locale: 'en-GB'` in the
  Playwright config the specs would assert English against a French app wherever
  the machine happened to be set to French.
- **The sidebar is asserted with `toHaveText`, never with a read into an array.**
  Reading the labels and comparing the array takes one snapshot, and a snapshot
  can be taken before the browser has applied the change the test just asked
  for. Angular is zoneless, so setting the language schedules change detection
  rather than doing it; the DOM catches up a tick later. Locally the read loses
  that race every time, because it runs on the same thread as the render. On a
  contended CI runner it does not, and the suite went red twice reading the
  language it had just switched away from. `toHaveText` polls, so a busy machine
  is slow rather than red.

## Not built yet

- OAuth 2 beyond the Keycloak resource server setup, as the ticket scopes it later.
- Languages beyond English and French. Adding one is a dictionary, a
  `messages_xx.properties`, a locale in `LocaleConfig` and an entry in the
  realm's `supportedLocales`.
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
- Changing the realm export does not change a realm Keycloak already has, since
  it only imports one that is absent. `node scripts/sync-realm.mjs` applies the
  export to a running Keycloak; `--wipe` is the alternative, at the cost of the
  application database.
- Shell scripts and the hook need the executable bit set in git itself
  (`git update-index --chmod=+x`). Windows checkouts run with
  `core.fileMode=false`, so a local `chmod` is not recorded, and a script
  committed without it fails on Linux and macOS, including in CI.
- The end to end stack uses the same ports as the development one, because the
  realm's redirect URIs name them, so the two cannot run at once.
- `docker/keycloak/themes/bms` hardcodes the registration URL, for the same
  reason `frontend/public/config.js` hardcodes the API host.
- The `bms-backend` client secret is a literal in the realm export, matched by a
  default in `application.yml`. Fine locally; a deployment has to set
  `BMS_KEYCLOAK_ADMIN_CLIENT_SECRET` and a realm to match.
- `frontend/public/config.js` is baked at image build, so changing the API or
  Keycloak URL needs an image rebuild rather than a container restart.
- Error messages already on screen are plain strings, so switching language
  leaves the last one in the language it was raised in. The next action replaces
  it.
- `docker/keycloak/themes/bms/login/messages` only holds the two keys the theme
  adds. Everything else on the sign in page comes from Keycloak's own bundles,
  which ship both languages; a third language would need only a realm setting
  and one small file.
- Native `<input type="date">` controls follow the browser's own locale, not the
  app's, so a date field can show a different separator from the dates in the
  table beside it.
