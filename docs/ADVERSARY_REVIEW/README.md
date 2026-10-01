# Adversary review (BM-18)

An adversarial security review of the application, read as code and then attacked
on a live production stack. The stack was `docker-compose.prod.yml` run locally
with `BMS_DOMAIN=localhost` and freshly generated secrets, torn down with its
volumes afterwards. Everything marked **confirmed live** was reproduced with
`curl` against that stack, signed in through the real Keycloak sign in flow.

Dates and line numbers are as of this branch. Nothing here was fixed; each
finding carries a suggested fix.

## The short version

- **Isolation between owners holds.** Every by-id lookup is scoped to the
  caller's accessible owners, so one owner cannot read or change another's
  buildings, apartments, tenants, expenses, invoices or reports. Tried live with
  foreign ids; all gave 404.
- **No SQL, template or header injection was found.** The invoice PDF escapes
  every field and loads no external resource.
- **The edge is tight.** The Keycloak admin console is refused, including through
  path tricks; the backend actuator and Swagger are not reachable from outside;
  anonymous dynamic client registration is refused.
- **But one owner can take over another owner's account**, and through it reach
  the administrator. This is the finding that blocks go live. Confirmed live.
- Registration and the assistant feature **leak which emails have accounts**,
  have **no rate limit**, mark **email verified without proof**, and **give every
  new email a fresh free trial**.
- Deleting a tenant, apartment or building **destroys issued invoices** with it,
  and a **sent invoice can be walked back to draft and deleted**. Both confirmed
  live, both matter for records kept for tax.

## Severity

| # | Finding | Severity | Status |
|---|---|---|---|
| 1 | Account takeover through assistant grant and password reset | Critical | Confirmed live |
| 2 | Deleting a tenant/apartment/building destroys its issued invoices | High | Confirmed live |
| 3 | Registration and grant enumerate accounts; no rate limit; endless trials | High | Confirmed live |
| 4 | Realm has no brute force protection, password policy or email verification | High | Confirmed by code |
| 5 | A sent invoice can be moved back to draft and deleted | Medium | Confirmed live |
| 6 | Email marked verified without proof, enabling assistant pre-hijack | Medium | Confirmed by code |
| 7 | Suspended owner's data stays readable by their assistants | Medium | Confirmed by code |
| 8 | A lapsed owner cannot revoke an assistant | Medium | Confirmed by code |
| 9 | Any role-less token is treated as an owner | Medium | Confirmed by code |
| 10 | No pagination; reports accept any date range | Low | Confirmed by code |
| 11 | CSRF skipped on any `Authorization` header, not only `Bearer` | Low | Confirmed by code |
| 12 | Password change needs no current password; "must change" not enforced | Low | Confirmed by code |
| 13 | `app_user.email` has no unique index | Low | Confirmed by code |
| 14 | Committed default client secret and datasource password | Low | Confirmed by code |

---

## 1. Account takeover (critical, confirmed live)

**Where:** `backend/src/main/java/com/bms/access/AssistantService.java:48-58`
(`grant`), `:69-72` (`resetPassword`); `identity/KeycloakAdminClient.java:86`;
the backend service account holds `manage-users` in
`docker/keycloak/realm-bms.json`.

`grant()` looks the invited email up with `findByEmailIgnoreCase`. If a local
account already exists, it is linked as an assistant with no check that it
belongs to someone else, no consent, and no check that it is not itself an owner
or the administrator. `resetPassword()` then resets that linked account's
password through Keycloak and returns the new password in the response.

**Reproduced live.** As a self-registered owner (`attacker@review.test`):

1. `POST /api/assistants {"email":"<a registered owner>", ...}` returned 201 and
   linked the victim.
2. `POST /api/assistants/{id}/password` returned
   `"temporaryPassword":"R4MBwYXbrvXU8q"`.
3. Signed in through Keycloak as the victim with that password and reached
   `/api/me` as the victim, with full owner rights.

**Important nuance found by testing.** The victim must already have a local
`app_user` row, which every owner gets the moment they register. An account that
exists only in Keycloak and has never signed in cannot be taken over this way:
granting the seeded `admin` before its first sign in returned 422, because the
lookup missed the local row and fell through to account creation. So the
administrator is reachable only once they have signed in at least once; every
registered owner is reachable immediately.

**Fix:**
- Only reset passwords for assistants this owner created. Record who created an
  assistant (a `created_by` column or a role), and refuse the reset otherwise.
- In `grant`, refuse to link an account that holds the owner or admin role, or
  that another owner already uses.
- Better, stop handing passwords through the owner at all: use Keycloak's
  execute-actions email so the assistant sets their own.

## 2. Deleting a parent destroys issued invoices (high, confirmed live)

**Where:** `backend/src/main/resources/db/migration/V1__init.sql:101-102`
(`invoice.apartment_id` and `tenant_id` are `on delete cascade`);
`tenant/TenantService.java`, `apartment/ApartmentService.java`,
`building/BuildingService.java` delete the parent row directly.

`InvoiceService.delete` refuses to delete anything but a draft. The cascade
ignores that rule.

**Reproduced live.** Created a tenant with a SENT invoice.
`DELETE /api/invoices/{id}` on the sent invoice was refused (422). Then
`DELETE /api/tenants/{id}` succeeded (204), and the sent invoice was gone (404).
An assistant with only `TENANT_WRITE` can do this; `APARTMENT_WRITE` and
`BUILDING_WRITE` wipe wider. Issued invoices feed the profit and loss statement
used for tax, so this is a records-integrity problem, not only a nuisance.

**Fix:** refuse to delete a tenant, apartment or building that has any non-draft
invoice, or soft delete instead of removing the row.

## 3. Registration and grant leak accounts, with no limits (high, confirmed live)

**Where:** `identity/AuthController.java:37-43`,
`config/SecurityConfig.java:50,83` (register is public and CSRF-exempt),
`identity/AccountService.java` (`startTrial`), and no rate limit in
`docker/caddy/Caddyfile`.

- **Enumeration, two ways.** `POST /api/auth/register` returns 422 "An account
  already exists" for a known email and 201 for a new one (both seen live). The
  assistant grant is a second oracle: a new email comes back with a
  `temporaryPassword`, an existing one with `null` (both seen live). Either can
  be scripted to harvest landlords' emails.
- **No rate limit** anywhere, so both the oracle and account creation can be run
  at speed.
- **Endless free trials:** every new email gets a fresh trial through
  `startTrial`.

**Fix:** rate limit registration per IP and email (a Caddy rate-limit module or
Bucket4j); return the same response whether or not the email exists; create the
account unverified and activate the trial only after verification.

**Not reproduced:** the companion code review suggested an oversized name leaves
an orphaned Keycloak account that blocks the email forever. Live, an oversized
name returned 502, but registering the same email again straight after
succeeded (201). The orphan was not observed; a `@Size(max=255)` on the
registration fields is still worth adding so the request fails cleanly rather
than at the database.

## 4. Realm has no hardening (high, confirmed by code)

**Where:** `docker/keycloak/realm-bms.json` has no `bruteForceProtected`, no
`passwordPolicy` and no `verifyEmail`; `RegistrationRequest` enforces only
`@Size(min=8)` on the password.

Credential stuffing is unthrottled at the identity provider, and passwords may
be weak. `docs/GO-LIVE-PART-1.md` lists these as pilot blockers, but the realm
export does not set them, and `scripts/sync-realm.mjs` does not add them.

**Fix:** set `bruteForceProtected:true`, a `passwordPolicy` such as
`length(12) and notUsername`, and `verifyEmail:true`; confirm the sync carries
them to an existing realm.

## 5. A sent invoice can be unwound and deleted (medium, confirmed live)

**Where:** `invoice/Invoice.java:109-117`. `transitionTo` only guards CANCELLED
(final) and PAID (only to CANCELLED). SENT to DRAFT is allowed.

**Reproduced live:** `status=SENT` (200), then `status=DRAFT` (200), then
`DELETE` (204). The "delete drafts only" rule is defeated and the invoice leaves
no trail.

**Fix:** a one-way transition table: DRAFT to SENT or CANCELLED, SENT to PAID or
CANCELLED, nothing back to DRAFT.

## 6. Email verified without proof (medium, confirmed by code)

**Where:** `identity/KeycloakAdminClient.java:86` creates users with
`emailVerified: true`.

An attacker can register a victim's email with their own password before the
victim is ever invited. When an owner later invites that email as an assistant,
finding 1 links the attacker's account, handing the owner's data to the
attacker. Fixing with real verification (finding 4) closes this.

## 7. Suspended owner's data stays readable (medium, confirmed by code)

**Where:** `access/AccessControl.java` checks suspension only for the caller, not
for the owner whose data is being read; `user/SuspensionInterceptor.java` gates
writes. After an admin suspends an owner, that owner's assistants keep read
access to buildings, tenant PII, invoice PDFs and reports. Decide whether
suspension is meant to lock the data; if so, skip suspended owners in
`accessibleOwnerIds`.

## 8. A lapsed owner cannot revoke an assistant (medium, confirmed by code)

**Where:** `access/AssistantService.java` runs `requireWritable` before revoke
and permission changes. An expired or suspended owner cannot remove a
compromised assistant, who keeps read access and regains write if the owner
renews. Exempt revoke and permission reduction from the writable check.

## 9. Role-less tokens are owners (medium, confirmed by code)

**Where:** `user/Roles.java` treats anyone without the assistant or admin role as
an owner. Safe only while a single confidential client issues tokens and
Keycloak registration is off. Any role-less principal, including the backend's
own service account, becomes a billable owner. Require an explicit `ROLE_OWNER`
on the owner endpoints and in `Roles.isOwner`.

## 10. No pagination; unbounded report ranges (low, confirmed by code)

List endpoints return every row and `GET /api/reports/profit-loss` accepts any
date range, loading all matching invoices and expenses into a shared heap.
Limited to the caller's own data, so low, but worth a `Pageable`, a maximum
range and `@Digits`/`@Size(max)` on amounts and line lists before large
portfolios arrive.

## 11. CSRF skipped on any Authorization header (low, confirmed by code)

**Where:** `config/SecurityConfig.java:82` exempts a request from CSRF whenever
an `Authorization` header is present, not only a `Bearer` one. Harmless while
CORS is single-origin (a cross-site custom header triggers a denied preflight),
but it should match `Bearer ` explicitly.

## 12. Password change and "must change" are weak (low, confirmed by code)

Changing a password needs no current password, so a stolen session is a
permanent takeover. The `mustChangePassword` flag is only read by the UI;
the API serves a new assistant indefinitely with the temporary password. Require
the current password, and block other endpoints while the flag is set.

## 13. No unique index on email (low, confirmed by code)

**Where:** `V1__init.sql` and `user/AppUserRepository.java`. If two local rows
share an email (possible if emails drift on profile sync),
`findByEmailIgnoreCase` throws and returns 500 on grant, registration and login.
Add a unique index on `lower(email)`.

## 14. Committed defaults (low, confirmed by code)

`application.yml` carries datasource `bms`/`bms` and client secret
`bms-backend-secret` for development. The prod compose requires the real secret
on Keycloak (`:?`) but not on the backend and sync services, where a missing
variable becomes an empty string rather than the default. Fail startup if the
secret is blank or equal to the default.

---

## What is done well

- **Owner isolation / IDOR.** Every by-id read, update and delete is scoped to
  `accessibleOwnerIds(permission)`; a foreign id gives 404. Nested creates are
  checked against the parent's owner. Updates cannot re-parent a row
  (`@JoinColumn(updatable=false)`).
- **The edge.** Tried live: `/auth/admin*` and `/auth/realms/master*` are 404,
  including through `//`, `%61`, `./`, case and `%2f` variants. The backend
  actuator and Swagger are not reachable through Caddy or nginx (`/api/...`
  requires auth; unknown paths fall back to the SPA `index.html`, which is why a
  path like `/.env` returns 200 with the app shell, not a file). Anonymous
  Keycloak dynamic client registration is refused ("Host not trusted").
  Security headers (HSTS, `X-Content-Type-Options`, `Referrer-Policy`,
  `X-Frame-Options: DENY`) are present.
- **Injection.** Every `@Query` uses named parameters; the one native query
  takes no user input. The invoice template uses `th:text` throughout, with no
  `th:utext`, no user markup and no base URI, so no template injection and no
  SSRF through the PDF.
- **Frontend.** No `innerHTML`, `bypassSecurityTrust*` or `DomSanitizer` use; no
  tokens in storage (only theme and language); the one redirect sink takes a
  sanitised path joined to the configured frontend URL
  (`config/LoginReturnPath.java` rejects `//` and `/\`), so no open redirect.
- **OAuth.** Authorization code with PKCE S256, `directAccessGrants` off,
  Keycloak self-registration off, tightly scoped redirect URIs, and a 14
  character `SecureRandom` generated password.

## One missing header

There is **no Content-Security-Policy** on the frontend responses. Everything is
served same-origin and there are no XSS sinks today, so this is defence in depth
rather than a live hole, but a strict CSP (`default-src 'self'`) in the Caddy
`handle` block or the nginx config would be cheap insurance.

## Suggested order of fixing

1. Finding 1 (account takeover) and finding 2 (cascade delete of invoices),
   before anyone else is invited.
2. Findings 3, 4 and 6 together: rate limit, realm hardening and real email
   verification, which also closes the pre-hijack.
3. Finding 5 and the rest as ordinary tickets.
