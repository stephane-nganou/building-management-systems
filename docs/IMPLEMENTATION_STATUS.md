# Implementation status

Last updated: 2026-10-01, after BM-28 (reading the containers' logs).
The security review behind BM-19 to BM-24 is in
[ADVERSARY_REVIEW/README.md](ADVERSARY_REVIEW/README.md).
The go live plan and the gaps it found are in [GO-LIVE-PART-1.md](GO-LIVE-PART-1.md).

The architecture is drawn out in
[ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md): containers, backend and
frontend structure, the domain model, the schema, and the sequence of every
main flow. This file records what is built and why it is built that way.

## Built and working end to end

| Area | State | Notes |
|---|---|---|
| Docker stack | Done | Postgres, Keycloak, backend and frontend; one command to start |
| Production stack | Done | `docker-compose.prod.yml`: one domain behind Caddy with Let's Encrypt, Keycloak in production mode under `/auth`, secrets from `.env.production`, nightly backups; see `docs/DEPLOYMENT.md` |
| Start and stop scripts | Done | mac, linux, windows |
| Container logs | Done | `scripts/logs.sh`, `logs-windows.ps1` and `logs-prod.sh` follow or save them; a Dozzle web viewer behind the `logs` compose profile |
| Database schema | Done | Flyway `V1__init.sql`, validated against JPA mappings by every integration test |
| Authentication | Done | The backend runs the authorization code flow and gives the browser a session cookie; other clients present a bearer token to the same API |
| Registration | Done | Public `/register` page and `POST /api/auth/register`; new users get the `owner` realm role |
| Sign in page | Done | Keycloak login theme in `docker/keycloak/themes/bms`, linking to our registration page; reached only because the backend redirects there |
| Assistant accounts | Done | The owner creates them; a password is returned once, and the app makes the assistant replace it on its own screen |
| Permission aware UI | Done | `canMatch` guards and a filtered sidebar; a denied screen is never downloaded |
| Owner and assistant access | Done | Per owner scoping plus 11 delegatable permissions |
| Subscriptions | Done | Dated periods per owner, a 30 day trial to start; outside every period the owner's data is read only, for them and their assistants |
| Administrator | Done | `admin` realm role and an **Accounts** screen: sign owners up, add or end periods, suspend and reactivate |
| Buildings | Done | Full CRUD, API and UI |
| Apartments | Done | Full CRUD with room layout, rent and status; unique label per building |
| Tenants | Done | Full CRUD, lease dates, deposit, one active tenant per apartment |
| Expenses | Done | Full CRUD, category, reason, optional apartment, filter by building and period |
| Invoices | Done | Rent and cold water, status flow, PDF download |
| Profit and loss | Done | Per building and total, expense breakdown by category; totals per currency |
| Currencies | Done | Each building keeps its books in EUR, XAF, XOF, USD, GBP or CHF; an invoice keeps the one it was issued in |
| Dashboard | Done | Portfolio counts, rent roll, year to date position |
| Visual design | Done | "Lights on": Mona Sans served from our own origin, a night and lamplight palette, and each building drawn as its floors with a lit window per let apartment. The sign in page and the invoice PDF match |
| Themes | Done | Classic, Magic and Ocean blue, chosen from the sidebar or the gate and remembered in the browser; Classic still follows the system's light or dark |
| English and French | Done | Every screen, API error, sign in page and invoice PDF |
| API documentation | Done | OpenAPI at `/swagger-ui.html` |
| Architecture diagrams | Done | `docs/ARCHITECTURE_DIAGRAMS.md`, Mermaid, rendered by GitHub |
| End to end tests | Done | Playwright against the real stack, run before every push and on every pull request |
| CI pipeline | Done | GitHub Actions: backend, frontend and end to end |

Verified against the running stack: sign in, create a building, apartment and
tenant, issue a rent invoice, download its PDF, record an expense and read the
profit and loss. For BM-3, against a throwaway stack: register from the browser,
sign in with the new account and see the `owner` role in its token, create an
assistant and confirm the account carries the `assistant` role, and confirm the
password is returned once and never again on a later read. For BM-8, against a
throwaway stack: sign in without the application ever naming Keycloak, walk
every screen while watching the wire for a call that leaves our own origin,
create an assistant and watch them replace the handed over password on our own
screen, and sign out at both ends. For BM-4, against a throwaway stack: switch the whole app
to French and back and watch it hold across a reload, land on a French
registration page from a French browser, read the Keycloak sign in page in both
languages, and download the same invoice as a French and an English PDF. For
BM-11, against the development database whose realm predated BM-8: sign in
failed with "Invalid parameter: redirect_uri", and after one start with the sync
service `bms-backend` carries its callback URI and the sign in form loads; the
end to end suite still passes on a throwaway stack. For BM-13, against the
development stack: every screen at 1440 wide in light and dark and at 390 wide
on a phone, each dialog, the delete confirmation and the drawer, the sign in page
in both languages, and an invoice PDF in both languages, read back with PyMuPDF
for its embedded fonts and rendered to check the layout. For BM-14, against the
development stack and its existing data: V3 and V4 applied, every non-assistant
got a trial, and the sync added the `admin` role and user to the realm on the
first start and skipped all six on the second. Signed in as `admin`, landed on
Accounts alone, ended the demo owner's subscription, then signed in as that
owner and saw the banner and a refused building with the backend's reason. The
Keycloak disable call was replayed against Keycloak 26.7 itself: a `PUT` of
`{"enabled": false}` answers 204 and leaves email, names and roles untouched.
For BM-12, against the development stack: every screen at 1440 wide in light
and dark and at 390 wide on a phone, shot before and after the colours moved
into tokens and compared pixel by pixel. Classic came out identical but for the
switcher's own row and 2/255 of anti-aliasing noise on one select, which two
shots of the same build also show. Then the same screens in Magic and in Ocean
blue, with the system set to light and to dark. For BM-5, against the
development stack and its existing data: V5 took the schema from v4 to v5 and
every existing building and invoice came out in euros. An owner with a Berlin
building in euros and a Douala one in CFA francs saw each figure on the
dashboard and the profit and loss as one line per currency, "FCFA 1,500,000"
fitting beside euros at 1440 and 1100 wide, and the building form in French
showed its currency as "euro (EUR)" with the warning that changing it converts
nothing. For BM-16, the production stack
on this machine with `BMS_DOMAIN=localhost`, where Caddy issues a certificate
of its own: every service healthy, only 80, 443 and Keycloak's console on
127.0.0.1 published, HTTP redirected to HTTPS, the issuer
`https://localhost/auth/realms/bms`, and `/auth/admin`, the admin API and the
master realm answering 404. In a browser the demo owner was refused, the
seeded administrator was made to choose a new password on the themed page and
landed on Accounts, and the session and forgery token cookies came back
Secure. A backup was taken, the application's users deleted, both dumps
restored with the commands in `docs/DEPLOYMENT.md`, and the administrator
signed in again with the password chosen before the backup. The development
stack still seeds its demo users, on a fresh realm and on an existing one, and
the end to end suite passes against it.

## Tests

- Backend: 50 tests. Unit tests for invoice totals, rounding and status rules;
  integration tests on a real Postgres 18 via Testcontainers covering the API,
  owner isolation, assistant permissions, registration, PDF rendering and the
  report maths. The language ones read the words back out of a rendered PDF
  with PDFBox, since a template that resolved no messages would still be a
  valid PDF. `AuthenticationIntegrationTest` covers the half a token cannot
  reach: a browser session established through `oidcLogin()` resolves to the
  same user and roles a bearer token does, an unauthenticated call is refused
  rather than redirected, the forgery token is required of a session and not of
  a token, and the handed over password obligation is set and cleared. The
  Keycloak admin client is mocked there. `ReadOnlyIntegrationTest` sends every
  write the API has as a lapsed owner and expects the refusal, and fails if a
  non-GET route exists that its list does not name, so a new write endpoint
  cannot quietly skip the check. `CurrencyIntegrationTest` covers a building's
  currency reaching everything under it, an invoice keeping its own after the
  building changes, whole francs on the line and on the PDF, and reports that
  keep one total per currency. `InputLimitsIntegrationTest` covers the bounds
  one request may ask for: amounts that fit their columns, the line cap, the
  one year report, and the paged lists with their size cap and ignored sort.
  Now 140 tests.
- Frontend: 58 unit tests, for the formatting pipes and per currency totals, the translation service and
  its dictionaries, the theme service, how a facade stacks apartments into floors and lights them,
  the delete confirmation and the toasts, and for the session and route guard logic that decides which
  screens exist, including that a refused profile is what "signed out" means and
  that an account owing us a password reaches no screen but the one that takes
  it, and that an administrator and a suspended account each see only theirs.
- End to end: 12 Playwright specs against the running stack, covering an
  administrator signing an owner up and ending their subscription, the link
  from the sign in page to registration, signing up and landing on a full
  portfolio, the duplicate email refusal, an owner creating an assistant who
  then has to choose a password and sees only their one granted screen, adding a
  building with an apartment in it, a building in CFA francs showing its rent in
  whole francs, switching the app to French and back,
  choosing a theme in the app and on the registration page and finding it kept
  after a reload, a
  French browser landing on a French registration page, and a walk through every
  screen that watches the wire and fails if any request leaves our own origin,
  a font or a stylesheet included, or anything at all reaches Keycloak, and
  another that fails on any Content-Security-Policy violation. `scripts/e2e` starts the
  stack on its own compose project, waits for every part, runs them and tears it
  down.

## Deliberate decisions

- **An invoice's status only moves forward (BM-22).** A draft may be sent or
  cancelled, a sent invoice paid or cancelled, a paid one cancelled (for a
  refund), and a cancelled one nothing. A sent invoice used to be able to go
  back to draft, and a draft is the one state that can be deleted, so an issued
  invoice could vanish without a trace. Draft straight to paid is refused too:
  an invoice is issued before it is paid. The table is `InvoiceStatus.canBecome`,
  and the interface already only offered the forward moves.

- **A new owner proves their address, and registration is rate limited (BM-21).**
  - *Realm:* brute force protection (ten failures lock an account, longer each
    time, up to 15 minutes), a password policy (12 to 128 characters, not the
    username or email), and `verifyEmail`. The sync carries all of it to an
    existing realm. The policy also applies to imported users, which is why the
    demo passwords are now `owner-demo-pass` and so on.
  - *Verification:* a self-registered owner is created unverified, and Keycloak
    itself stops their first sign in and emails the link; the backend sends no
    email. Owners an administrator creates and assistants an owner creates stay
    verified: someone who knows them typed the address and handed the password
    over. Opened in the same browser, the link finishes the sign in; opened on
    another device, Keycloak asks for one click and offers a way back to the
    application, which is the client's `baseUrl`.
  - *Mail:* Mailpit catches everything in development and the end to end suite,
    which reads it through its API. Production needs an SMTP relay (`BMS_SMTP_*`)
    and will not start without one.
  - *Password set atomically:* the account and its password are now created in
    one Keycloak call, so a password the policy refuses leaves no account behind.
    Our validation mirrors the policy, so the refusal is explained before
    Keycloak is asked.
  - *Rate limit:* `POST /api/auth/register` allows 5 sign ups per client address
    per hour (`bms.registration.max-per-hour`), then answers 429. It is an
    interceptor, so the refusal is translated like every other error; the
    counts live in memory, which only holds for one backend replica.
  - *Kept on purpose:* "An account already exists" stays, so a real user is told
    why; the rate limit makes harvesting emails through it slow.

- **Issued records outlive what they belong to (BM-20).** Deleting a tenant,
  apartment or building used to cascade away its invoices, sent and paid ones
  included, and a building its expenses, although the profit and loss statement
  for tax is built from them. Such a delete is now refused with a message saying
  why; a tenant who left is marked inactive instead. Draft invoices were never
  issued and still go with their parent, deleted by the service first. The
  foreign keys from `invoice` to `tenant` and `apartment`, and from `expense` to
  `building`, are `on delete restrict` (V8), so the database refuses the same
  delete whatever the code path. A cancelled invoice counts as issued: its
  number was used. An apartment's expenses still stay, unlinked, when it goes.

- **An account carries its role, and an assistant its creator (BM-19).** An
  owner could add any existing account as their assistant and then reset its
  password, which handed them any other owner's account and, through it, the
  administrator. The local record now keeps the account's role (set at creation
  and refreshed from the token on every sign in) and, for an assistant, the
  owner who created it. An owner or administrator can no longer be linked as an
  assistant, and only the owner who created an assistant may reset its password.
  A shared assistant is still linked by other owners, but only the creator holds
  the password reset. This closes finding 1 of the adversary review. V7 then
  corrects V6: an account holding a subscription period is an owner even if it
  was linked as someone's assistant, which is exactly the takeover being closed;
  and deleting the owner who created an assistant clears the creator instead of
  being refused.

- **Income counts `SENT` and `PAID` invoices**, by issue date. Drafts and
  cancelled invoices are excluded. Report totals are aggregated in memory so
  they always match the rounded per line amounts printed on the invoice.
- **Invoice numbers come from a Postgres sequence** (`INV-<year>-<6 digits>`),
  which stays unique under concurrent creation. A count based scheme would race.
- **A user record is created by a servlet filter**, before any request scoped
  transaction opens. Creating it lazily inside a service silently did nothing
  when the caller's outermost transaction was read only, because Hibernate does
  not flush there.
- **Keycloak's `basic` client scope is required.** Without it the token carries
  no `sub` claim, which is the only stable user identifier.
- **Accounts are created through the Keycloak admin REST API** with a plain
  `RestClient`, driven by the `bms-backend` client's service account. The official
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
  initializer here bounced the browser between the app and the sign in page for
  ever. Guards run after bootstrap and can await an answer. The end to end suite
  is what caught this.
- **A failed profile request is not remembered**, so the next guard tries again
  rather than stranding the user on the empty page because the backend was a
  moment slower to start than the browser.
- **The sign in page is themed rather than replaced.** It is the one page of
  Keycloak anyone still sees, and the browser reaches it because the backend
  redirected there, not because the application knew where to send it.
  Keycloak's own registration stays off, because signing up has to assign the
  owner role, which only our backend does. The theme copies one 56 line template from
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
- **Only the `owner` role makes an owner (BM-23).** A missing role used to count
  as owning, for accounts made before roles existed; none exist now, and the
  fallback made any role-less principal, the backend's own service account
  included, an owner with a trial. A role-less account is now `NONE`: it owns
  nothing, gets no trial, and cannot be linked as an assistant. Managing
  assistants and starting a building need the role in the security
  configuration itself, rather than failing only because a non-owner has no
  subscription.
- **Suspension closes an owner's data to their assistants too (BM-23).** The
  assignment queries skip suspended owners, so every screen loses them at once;
  `/api/me` still lists the delegation, with its status, so the assistant is told
  why. An expired subscription stays as it was: readable, not changeable.
- **A lapsed owner can still cut an assistant off (BM-23).** Revoking, or
  narrowing permissions, only reduces risk, so neither needs a writable
  subscription; widening them, adding an assistant or resetting a password does.
- **Only a bearer token excuses the forgery token (BM-24).** Any `Authorization`
  header used to skip the CSRF check, while a session cookie on the same request
  still authenticated it. Proved first: a session with `Authorization: Basic ...`
  and no token wrote a password (204). The header must now start `Bearer `, read
  case insensitively as the bearer token resolver reads it.
- **One account per email, ignoring case (BM-24).** `V9__unique_email.sql` adds a
  unique index on `lower(email)`, since every lookup is `findByEmailIgnoreCase`
  and two rows would make each one fail. The migration fails on a database that
  already holds such a pair; none should, as Keycloak refuses duplicate emails.
- **No start with a known client secret (BM-24).** `KeycloakProperties` refuses a
  blank secret, and the committed `bms-backend-secret` unless
  `BMS_KEYCLOAK_ALLOW_DEFAULT_SECRET=true`, which the development compose file
  and the integration tests set and production never does. The production
  compose file now guards every use of a secret with `:?`, not only the first;
  compose already failed on the first, but nothing should hang on its position.
- **Changing a password needs the current one (BM-24).** Checked by signing in
  with it through `bms-password-check`, a confidential client with direct access
  grants and nothing else, holding the same secret as `bms-backend`: one process
  holds both, so a second secret would separate nothing. The session that check
  opens is ended at once, and a wrong guess counts towards the realm's lockout
  like any other. Only `invalid_grant` is the user's mistake; `invalid_client`
  means the realm lacks the client and is logged as our fault. The sync service
  writes the client into an existing realm, and now only signs in as clients
  with a service account. For an account handed a password, the current one is
  the one it was handed.
- **The API holds an account to choosing its password (BM-24).** The UI kept
  such an account on the password screen, but the API served it with the handed
  over password indefinitely. `PasswordChangeInterceptor`, beside
  `SuspensionInterceptor`, refuses it everything but `/api/me` and
  `/api/auth/password`.
- **Bounded requests (BM-24).** Money takes `@Digits(10, 2)`, quantity
  `@Digits(9, 3)` and size `@Digits(6, 2)`, matching their columns, so an
  oversized amount is a 400 instead of a numeric overflow and a 500 (proved
  first). An invoice takes at most 50 lines. A profit and loss report spans less
  than a year from its start, a whole leap year included. Invoices and expenses
  come a page at a time as Spring Data's `PagedModel` (default 50, at most 200),
  newest first with the id as tie-breaker; a `sort` parameter is ignored, since
  it would otherwise be appended to the JPQL. The report keeps its own unpaged
  queries. Buildings, apartments and tenants stay whole lists: they fill
  dropdowns and the dashboard, and are bounded by a portfolio's size. The
  expenses screen totals only the page on screen, and says so once there is
  more than one.
- **A strict Content-Security-Policy on the app (BM-24).** nginx sends it with
  `index.html`, so the development stack and production (through Caddy) are the
  same, and Keycloak's own pages under `/auth` keep Keycloak's policy. The theme
  script in `index.html` is allowed by its hash. Angular's critical CSS inlining
  is off: it loads the stylesheet through an `onload` handler the policy blocks.
  `e2e/csp.spec.ts` fails on any violation, which is how a changed theme script
  with a stale hash would show.
- **Logs are read where Docker keeps them (BM-28).** No log shipping and no
  files written by the services: every container already logs to Docker,
  rotated in production, so the scripts wrap `docker compose logs` (follow, or
  save to a git-ignored `logs/`), and the web view is Dozzle, which reads the
  same logs through the Docker socket. It sits behind a compose profile,
  `logs`, off by default and switched on with `COMPOSE_PROFILES=logs` in
  `.env` or `.env.production`, which Compose reads itself, so no start script
  needed a flag; `--viewer` starts it on demand. It answers on localhost only,
  production reaching it over an SSH tunnel like Keycloak's console, and shows
  only this stack's containers: `name=bms-` in development, the `hausbuch`
  project label in production (checked: a stray container stays hidden).
  Dozzle's container actions and shell stay at their default, off. A plain
  `docker compose down` leaves a profile service running (checked), so the
  stop scripts name the profile.
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

- **The browser is signed in by the backend, not by the page.** The application
  navigates to `/api/auth/login/keycloak` and Spring Security runs the
  authorization code flow, so the Angular source names no identity provider,
  carries no identity library and holds no token. What it gets back is an
  `HttpOnly` session cookie it cannot read.

  The alternative was a sign in form of our own posting to the backend, which
  would have been prettier. It needs the resource owner password credentials
  grant, which OAuth 2.1 removes outright: it hands the user's password to the
  client, cannot carry multi factor or federation, and trains people to type
  credentials into pages the identity provider did not serve. Redirecting from
  the backend satisfies "the frontend knows only the backend" without giving
  that up.

- **No token in the page, deliberately.** The browser based apps guidance is
  blunt that nothing JavaScript can read survives cross site scripting, and that
  the storage choice, `localStorage` or memory or IndexedDB, changes nothing. A
  backend for frontend does not prevent that either: an injected script can act
  as the user for as long as the tab is open. What it prevents is exfiltration,
  a credential carried off and used elsewhere, later. Containment, not immunity.

- **The API is proxied, not published.** nginx serves the bundle and forwards
  `/api` to the backend, so the application and its API are one origin. Three
  things follow that are awkward otherwise: the session cookie needs no
  `SameSite=None`, CORS leaves the browser's path entirely, and Angular's own
  forgery protection applies, since it only adds its header to relative URLs.
  `frontend/proxy.conf.json` does the same for `ng serve`.

- **Forgery protection is on, and exempts bearer callers.** A session cookie is
  ambient: the browser attaches it whether or not the user meant to make the
  request. The token cookie a script has to read and echo is what proves intent.
  A caller holding a bearer token brought its own credential, which no other site
  can make a browser attach, so it is exempt. The plain request handler is used
  rather than the XOR default, whose encoding assumes the token is rendered into
  a server side template; here it has to survive a round trip through a cookie
  unchanged.

- **The OAuth2 client registration is written out, not discovered.** An
  `issuer-uri` would make Spring fetch Keycloak's discovery document at startup,
  which couples boot order to Keycloak being up. Worse, discovery yields one set
  of URLs and this deployment needs two: the browser is sent to
  `http://localhost:8081` while the code exchange, userinfo and keys go over the
  container network. `KeycloakClientConfig` sets each endpoint to the right one
  of the two, and supplies `end_session_endpoint` by hand so signing out can
  still end the session at Keycloak.

- **Realm roles are put into the ID token by a mapper in the realm export.**
  Keycloak writes them to the access token by default but not the ID token, and
  the ID token is what backs a browser session. Without that mapper every signed
  in owner would arrive holding no role, and since an owner is anyone whose token
  does not say `assistant`, the failure would have been silent rather than loud.

- **`CurrentUserService` reads claims, not a token type.** A browser's principal
  is an `OidcUser` and a mobile client's is a `Jwt`; both are `ClaimAccessor`s
  over the same Keycloak claims. Unifying on that interface is what lets one API
  serve both without a single service, permission check or existing test knowing
  which arrived.

- **The obligation to change a handed over password is ours, not Keycloak's.**
  Keycloak discharges a required action on its own account pages, which is
  precisely where this application no longer sends anyone. `must_change_password`
  on `app_user` is set when an owner creates or resets an assistant, reported by
  `/api/me`, and cleared by `POST /api/auth/password`. That endpoint does not ask
  for the current password: proving it would mean sending it to Keycloak through
  the direct access grant we deliberately leave disabled, and the caller has
  already proved as much as that would.

- **`AccountService.changePassword` takes an id, not the record.**
  `CurrentUserService.require` reads the user in a transaction of its own, which
  has committed by the time the writing one begins. Flipping the flag on that
  detached entity would take the change no further than memory.

- **The sign in return path is a path, never a URL.** It is stashed in the
  session on the way out and joined to the configured frontend address on the way
  back, and anything not beginning with a single slash is dropped. A value that
  cannot name a host cannot turn our sign in link into someone else's redirect.
  `ui_locales` is checked against the two languages we have, for the same reason.

- **An unauthenticated request gets 401, never a redirect.** A 302 towards
  another host is unreadable to a background request: the browser follows it and
  hands back an opaque failure, so the application could not tell "signed out"
  from "broken". The application turns that 401 into a sign in itself, once,
  guarded by a flag so that a refused request and the guard behind it do not each
  start a navigation.

- **The diagrams are Mermaid in Markdown, not image files.** GitHub renders them
  in the browser, so no toolchain is needed to read one, and a change shows up
  as a readable diff rather than a new binary. An exported PNG or a `.drawio`
  file would need a round trip through a tool nobody has installed, which is
  how diagrams stop being updated. Note that `;` terminates a statement in a
  Mermaid sequence diagram, so it cannot appear inside message text.

- **The realm export is applied on every start (BM-11).** `--import-realm`
  skips a realm that already exists (`Strategy: IGNORE_EXISTING`), so a stack
  first started before BM-8 kept a `bms-backend` client with no redirect URIs,
  and Keycloak refused the backend's callback with "Invalid parameter:
  redirect_uri". The one-shot `keycloak-sync` compose service (`node:24-alpine`)
  now runs `scripts/sync-realm.mjs` against `http://keycloak:8080` once Keycloak
  is healthy, and the backend waits for it with
  `service_completed_successfully`. A plain `docker compose up` gets it too, and
  users are left untouched. Wiping the volume was rejected because the same
  Postgres holds the application data.

- **The facade is data, not decoration (BM-13).** One drawing carries the
  identity: a building as its floors, one window per apartment, lit when let,
  dark when vacant, struck through under works. Lamplight is kept for that
  meaning and for keyboard focus, and nothing else is amber. The dashboard street
  needs `BUILDING_READ` and `APARTMENT_READ`; without both it shows the per
  building table instead, and the building list drops its thumbnails, rather than
  drawing buildings as empty.

- **Dialogs are the native `<dialog>` opened with `showModal` (BM-13).** It
  traps focus, closes on Escape and hands focus back for free. `Dialog` focuses
  the first field, or the first answer when there is none, so a confirmation
  starts on Cancel. A backdrop click closes only when the press began on the
  backdrop too, because a drag out of a field is also delivered to the dialog.
  A failed save shows inside the dialog, since the page behind it is covered.

- **Tabular figures are off (BM-13).** Mona Sans draws them as a monospace set
  with a slashed zero, which read as a second typeface inside every table.
  Right aligned proportional figures still line up well enough for money.

- **The invoice PDF embeds Mona Sans without its substitution table (BM-13).**
  PDFBox 3 applies the font's `liga` feature as it writes, so "ti", "tt" and
  "ff" became single ligature glyphs with no character behind them, and text
  copied or searched out of the invoice lost those letters ("Désigna on").
  `InvoicePdfRenderer` loads each static cut itself, drops `GSUB` and hands
  the fonts to openhtmltopdf through a document it owns. The language test now
  looks for those words.

- **The sign in page is restyled by one sheet on top of keycloak.v2 (BM-13).**
  `theme.properties` lists the parent's `css/styles.css` then ours, and
  PatternFly 5's own variables do most of the work, so no Keycloak template
  beyond `login.ftl` is copied. It is pinned to light, the card on the night
  street, as the app's own register page is. The realm is now shown as
  Hausbuch.

- **A subscription is a history of dated periods, both ends included (BM-14).**
  An owner may change their data on a day some period covers; outside every
  period it is read only, for them and for every assistant working for them.
  There are no plans or prices, and no payment: an administrator records what
  was paid for. "Extend" is adding the next period, so the history stays.

- **An owner is anyone who has ever had a period (BM-14).** Roles live in
  Keycloak, not in our database, so the administrator's list needs a mark of
  its own. Every way an owner comes to exist gives them a trial: registration,
  an administrator signing them up, and the first request of an owner made in
  Keycloak directly, the seeded demo owner among them. Nothing ever deletes a
  period, because that would take the owner off the list where they are
  renewed. Ending a period on the day it began cuts it to end the day before it
  starts, which covers no day, and V4 lets the check constraint hold that.

- **The migration had to guess, and the token corrects it (BM-14).** V3 gave a
  trial to everyone who is nobody's assistant, which also caught an assistant
  never assigned or since revoked. When anyone whose token does not make them
  an owner makes a request, any period they hold is removed.

- **Read only is enforced where each record is loaded for writing (BM-14).**
  Every write already went through its service's `require(id, permission)`
  with a `*_WRITE` permission, and that now asks
  `AccessControl.requireWritable` about the record's owner. Building creation
  and assistant management act on the caller's own account and ask directly.
  It is per owner: an assistant for one lapsed and one paying owner keeps
  working for the second. The refusal is a 403 whose `detail` explains it, and
  the screens now show `detail` when a change is refused, where several showed
  only a generic line.

- **Suspension is refused in an interceptor, not by Keycloak alone (BM-14).**
  Disabling the account in Keycloak stops new sign ins, but a browser session
  lives in the backend and would carry on. `SuspensionInterceptor` refuses a
  suspended account every endpoint but `/api/me`, which the application needs
  in order to show why. An interceptor rather than a filter, because it runs
  after the request's language is resolved and its exception reaches
  `ApiExceptionHandler` like any other. Keycloak is changed first, so a refusal
  there leaves our record as it was.

- **An administrator is not an owner (BM-14).** `Roles.isOwner` treats a token
  that says `admin` like one that says `assistant`, so an administrator gets no
  trial, no owner screens and nothing they could change. `/api/admin/**` needs
  `ROLE_ADMIN` in `SecurityConfig`, the first role rule the chain has.

- **The sync adds seeded users that are missing (BM-14).** The seeded
  administrator came after realms already existed. Users go through the same
  `SKIP` pass as roles, so a missing one is created and an existing one, with
  its id and its buildings, is never replaced. The service account is left to
  its own step.

- **A theme is a set of tokens, not a stylesheet of its own (BM-12).** Every
  colour the rail, the street, the toast, the focus ring and the logo used to
  hardcode is now a token in `tokens.css`, and `themes.css` overrides them
  under `:root[data-theme]`, with radii, shadows and a texture. One stylesheet
  per theme would have repeated some 1,400 lines and fetched on every switch.
  The lit window keeps its meaning in all three: candlelight under the stars
  in Magic, noon sun over the harbour in Ocean blue. Neither new theme follows
  the system's light or dark setting, so Classic's dark block names the others
  to stay out of their way, and a fourth theme has to be added there too.

- **The theme is set before the first paint (BM-12).** A one line script in
  `index.html` copies `bms.theme` from `localStorage` onto `<html>`, so a
  dark theme never flashes light while the bundle loads. `ThemeService` owns
  the choice after that. It lives in the browser, like the language: a new
  browser starts on Classic.

- **Currency belongs to the building, not the owner (BM-5).** An owner may hold
  buildings in different countries, and an assistant may work for owners in
  different ones. Every response with an amount carries the currency of the
  building it belongs to, and the `money` pipe is given it rather than
  assuming one. Totals are kept per currency, on the dashboard, in the profit
  and loss and under the expense list, and never added across them: there are
  no exchange rates anywhere. A figure holding more than one currency is set a
  step smaller, because a rent roll in CFA francs runs to millions.

- **An invoice keeps the currency it was issued in (BM-5).** It is copied from
  the building when the invoice is created and is not updatable, so changing a
  building's currency relabels its rent, deposits and expenses, as the form
  warns, but never a document a tenant already has. Its lines round to that
  currency's own digits, none for the CFA francs, so the total printed is the
  sum of the lines printed. The deployment wide `bms.invoice.currency` setting
  is gone.

- **The currency list is closed (BM-5).** `CurrencyCode` in the backend and
  `CURRENCIES` in `models.ts` name the same six. An unknown code is a 400.
  Taking every ISO 4217 code would mean trusting that each one renders on the
  PDF, which nobody has looked at.

- **One realm definition, and the people are per environment (BM-16).** The
  export keeps the realm, its roles, the client and the service account. The
  client secret and the redirect URIs are `${NAME:default}` placeholders
  whose defaults are the development values, which Keycloak resolves on
  import and `sync-realm.mjs` resolves the same way, except that it refuses a
  placeholder with neither a value nor a default. The demo users moved to
  `users-demo.json` and production's single administrator is in
  `users-prod.json`, seeded by the sync from `KEYCLOAK_USERS_FILE`. A second
  realm file for production was rejected: two copies of the client and its
  mappers drift. The administrator must change their password at the first
  sign in through `requiredActions`; a credential marked `temporary` in a
  partial import is not enforced, which was found by signing in.

- **One domain in production, Keycloak under `/auth` (BM-16).** Caddy sends
  `/api` straight to the backend rather than through the frontend's nginx,
  which would overwrite `X-Forwarded-Proto` with its own `http`. The backend
  trusts forwarded headers (`SERVER_FORWARD_HEADERS_STRATEGY=framework`), so
  its cookies are Secure; it builds every redirect from
  `BMS_FRONTEND_BASE_URL` either way. Keycloak runs `start` with
  `KC_HTTP_RELATIVE_PATH=/auth`, which moves its health endpoint on port 9000
  to `/auth/health` too. Its admin console is refused at the edge and bound to
  127.0.0.1, reached over an SSH tunnel with `KC_HOSTNAME_ADMIN` pointing
  there.

- **Backups are dumps on the same host (BM-16).** `pg_dump` of both databases
  on start and at 02:00 UTC, kept 14 days, written aside and moved into place
  so a partial file never looks complete. Getting them off the server is left
  to the operator and said so in the deployment guide; both have to be
  restored together, because application records are keyed on Keycloak ids.

- **A bare date is a calendar day (BM-14).** `new Date('2026-10-29')` is UTC
  midnight, which is the 28th anywhere west of Greenwich, so `DayPipe` read
  every due date, lease date and subscription end a day early there. A date
  with no time is now read as local midnight.

## Not built yet

- Payment. An administrator records periods by hand; nothing charges an owner
  or renews a subscription on its own, and nobody is told before one runs out.

- Sender constrained tokens. DPoP would make a stolen access token useless to
  anyone but its holder, which is the remaining hardening for the clients that
  do carry one. The browser does not, so it gains nothing there.
- A mobile client. The API accepts a bearer token today and the realm would
  need a public client with PKCE for one to exist.
- Languages beyond English and French. Adding one is a dictionary, a
  `messages_xx.properties`, a locale in `LocaleConfig` and an entry in the
  realm's `supportedLocales`.
- An Impressum and a privacy policy, and a feedback link in the app; all three
  are needed before the pilot in [GO-LIVE-PART-1.md](GO-LIVE-PART-1.md).
- Deleting an account or exporting its data from the app. Until then it is done
  by hand on request.
- Emailing invoices to tenants; today they are downloaded as PDF.
- Cold water meter readings; a cold water invoice takes its lines directly.
- Attachments or receipts on expenses.
- Pagination. Every list returns all rows, which is fine at a landlord's scale
  but should be revisited before large portfolios.
- Component level frontend tests. The pipes, session and guards have unit tests
  and the main journeys have end to end ones, but individual screens do not.
- End to end coverage of invoices and the profit and loss report.

## Known rough edges

- The architecture diagrams are kept by hand and nothing checks them against the
  code, so a change to the domain model or a flow has to be carried into
  `docs/ARCHITECTURE_DIAGRAMS.md` deliberately.

- Write actions are still shown inside a screen an assistant may only read; the
  backend refuses them, but the buttons are there.
- Signing out is a `GET`, so that ending the Keycloak session stays one browser
  navigation. A forged sign out is possible and costs the user nothing beyond
  the annoyance of signing in again.
- Keycloak only imports a realm that is absent, so a changed export reaches an
  existing realm through the `keycloak-sync` service on the next start (BM-11),
  or through `node scripts/sync-realm.mjs` against a running Keycloak. The sync
  never removes anything, and it adds roles but never overwrites them.
- Shell scripts and the hook need the executable bit set in git itself
  (`git update-index --chmod=+x`). Windows checkouts run with
  `core.fileMode=false`, so a local `chmod` is not recorded, and a script
  committed without it fails on Linux and macOS, including in CI.
- The end to end stack uses the same ports as the development one, because the
  realm's redirect URIs name them, so the two cannot run at once.
- The `bms-backend` client secret defaults to `bms-backend-secret` in the
  realm export and in `application.yml`, for development. The production
  compose file refuses to start without `BMS_KEYCLOAK_CLIENT_SECRET`, but a
  deployment put together some other way would inherit the default.
- The production stack builds its images on the server from a checkout. There
  is no registry and no image tag to roll back to; going back is a
  `git checkout` and another start.
- Production has no alerting: nothing reports a failed backup, a certificate
  Caddy could not renew or a service that keeps restarting. `docker compose
  ps` and the logs are the only view.
- The session lives in the backend's memory. A second replica would hand a
  browser a session the other one has never heard of, so scaling out needs
  Spring Session backed by Postgres or Redis before it can work.
- Removing the `bms-frontend` client from the realm export does not remove it
  from a realm Keycloak already has: `scripts/sync-realm.mjs` overwrites the
  clients in the export and leaves anything else alone. It is inert either way,
  since nothing holds its id any more.
- Mona Sans ships three times: as variable woff2 in `frontend/public/fonts`
  and in the Keycloak theme, and as static TTF cuts in the backend for the PDF,
  which cannot read variable fonts. The palette is likewise repeated in the
  theme's `bms.css` and the invoice template. A change to the look has to be
  made in all three.
- The Claude in Chrome screenshot tool times out while a native modal dialog is
  open, although the page itself keeps responding. Screenshots of dialogs were
  taken with a scratch Playwright script instead.
- Error messages already on screen are plain strings, so switching language
  leaves the last one in the language it was raised in. The next action replaces
  it.
- `docker/keycloak/themes/bms/login/messages` only holds the two keys the theme
  adds. Everything else on the sign in page comes from Keycloak's own bundles,
  which ship both languages; a third language would need only a realm setting
  and one small file.
- "Today" for a subscription is the backend's clock, which is UTC in the
  container, so a period runs out at midnight UTC rather than at the owner's own
  midnight.
- The administrator's list asks for each owner's standing and counts one owner
  at a time, a handful of queries each. Fine for tens of customers, worth one
  grouped query before thousands.
- A write button stays on screen while an owner's data is read only; pressing it
  shows why it was refused, and the banner says so up front.
- The sign in page and the invoice PDF stay Classic whatever the theme. The
  sign in page is Keycloak's and cannot read our storage, and an invoice is a
  document for the tenant rather than a view for the owner.
- The currency list lives twice, as `CurrencyCode` and as `CURRENCIES` in
  `models.ts`. Adding one means both, and a look at the invoice PDF.
- Native `<input type="date">` controls follow the browser's own locale, not the
  app's, so a date field can show a different separator from the dates in the
  table beside it.
