# Go live, part 1: a free pilot in Germany

The aim is feedback from real landlords as soon as possible, for as little money
as possible. The first go live is therefore a **free pilot**: 5 to 10 landlords
in Germany, signed up by hand, using the product for 8 weeks while we talk to
them. Payment, scaling and polish come after we know what they need.

The product is close. BM-16 already gives a production stack on one server
behind HTTPS, with nightly database dumps, and every feature a landlord needs
for a first month: buildings, apartments, tenants, expenses, invoices as PDF,
profit and loss, assistants, English and French. What is missing is mostly
around the product: legal pages, email, security settings, monitoring and a
way to hear from users. Section 3 lists it.

Prices below are approximate, as of October 2026. Check them when buying.

## 1. Running cost

| Item | Choice | Per month |
|---|---|---|
| Server | Hetzner Cloud, 2 vCPU, 4 GB RAM, EU location (Falkenstein or Nuremberg) | about 5 EUR |
| Off-site backups | Hetzner Storage Box, smallest size | about 4 EUR |
| Domain | `.de` or `.com` at any registrar | about 1 EUR |
| Transactional email | Brevo free plan (300 emails a day, EU company) | 0 |
| Uptime monitoring | UptimeRobot free plan (5 minute checks, email alerts) | 0 |
| Backup heartbeat | Healthchecks.io free plan | 0 |
| Feedback form | Tally free plan (EU company) | 0 |
| **Total** | | **about 10 EUR** |

4 GB of RAM is the floor, not a luxury: Keycloak and the backend each want
around 1 GB, Postgres and Caddy a few hundred MB, and the images are built on
the server, where the Angular build alone needs well over 1 GB for a minute.

Not worth paying for yet: a staging server (the end to end stack run locally
covers that), a managed database, a container registry, analytics, error
tracking. Each is listed in section 3.4 with the moment it starts to pay off.

## 2. Step by step

### Step 1: decide the name and the domain (day 1)

- Pick the product name. `.env.production.example` uses "Hausbuch". Search the
  German trademark register ([DPMAregister](https://register.dpma.de)) and the
  EU one ([TMview](https://www.tmdn.org/tmview)) for the name in the software
  class (9 and 42) before printing it anywhere.
- Buy the domain. Everything is served from it, Keycloak under `/auth`.

### Step 2: close the product gaps that block a pilot (week 1)

Each of these is a small ticket. They are in section 3 with the reasons.

1. **Email from Keycloak.** Configure an SMTP server in the realm (Brevo's SMTP
   relay), with the credentials in `.env.production`. Without it a pilot user
   who forgets their password is locked out, and only we can let them back in.
   `resetPasswordAllowed` is already on; the link simply cannot be sent today.
2. **Brute force protection and a password policy** in `realm-bms.json`:
   `bruteForceProtected: true`, and a policy of at least 12 characters. Check
   that the sync carries them to the existing realm, or set them in the admin
   console as well.
3. **Legal pages** reachable from every screen and from the registration page:
   Impressum and Datenschutzerklärung (section 3.1). Static pages in the
   frontend are enough.
4. **A feedback link** in the app's header: "Send feedback" opening the Tally
   form, with the user's email prefilled. One place to collect everything.

### Step 3: set up the server (week 1, half a day)

1. Create the server with an SSH key, no password. Choose an EU location.
2. Harden it a little:
   - SSH: keys only, no root login (`PermitRootLogin no`,
     `PasswordAuthentication no`).
   - `unattended-upgrades` for security updates.
   - Hetzner's cloud firewall: allow 22, 80 and 443 only. Use it rather than
     `ufw`, which Docker's published ports bypass.
3. Install Docker and the Compose plugin, point the domain's `A` and `AAAA`
   records at the server, and follow [DEPLOYMENT.md](DEPLOYMENT.md) to the end.
4. Sign the Hetzner data processing agreement (AVV) in the Hetzner console. Do
   the same with Brevo.

### Step 4: backups we have actually restored (week 1)

The stack writes dumps to `backups/` on the same disk, which is no backup
against losing the server.

1. Copy `backups/` to the Storage Box every night (`rsync` over SSH from a cron
   job on the server, or `rclone`).
2. **Restore once, for real**, on a throwaway server or locally, with the steps
   in DEPLOYMENT.md. Sign in afterwards and open a building. A backup that was
   never restored is a hope, not a backup.
3. Put a reminder in the calendar to repeat it every month of the pilot.

### Step 5: monitoring (week 1, an hour)

There is no alerting in the stack (see Known rough edges). For a pilot, outside
checks are enough:

- UptimeRobot on `https://<domain>/` and on the sign in page
  `https://<domain>/auth/realms/bms/.well-known/openid-configuration`, alerting
  by email.
- A heartbeat on [Healthchecks.io](https://healthchecks.io) (free plan): the
  nightly copy to the Storage Box pings it when it succeeds, and it emails us
  when a night passes without a ping. That catches backups that silently
  stopped, which the monthly restore would only find weeks later.
- Let's Encrypt writes to `ACME_EMAIL` about certificates that are close to
  expiring. Make it an address someone reads.

### Step 6: legal and paperwork (week 1 to 2, in parallel)

See section 3.1. In short: Impressum, privacy policy, a short pilot agreement
with terms of use and a data processing agreement, and a record of processing.

### Step 7: rehearse the whole thing (end of week 2)

On the live server, before inviting anyone:

1. Sign in as the administrator, change the password, sign a test owner up.
2. As that owner: add a building, an apartment, a tenant, an expense, an
   invoice; download the PDF; open the profit and loss report; add an
   assistant and sign in as them.
3. Use "forgot password" and check that the email arrives and is not spam.
   Brevo needs SPF and DKIM records on the domain for that.
4. Do all of it once in French and once on a phone.
5. Delete the test owner (section 3.2 describes how, by hand).

### Step 8: invite the pilot users (week 3)

- Recruit 5 to 10 landlords from your own network: a mix of one building and
  several, at least one with an assistant (a family member or a property
  manager), at least one who works in French.
- Sign each up yourself from **Accounts**, and extend their subscription to the
  end of the pilot plus a margin. The default is a 30 day trial, and nobody is
  warned before it runs out: on day 31 a pilot user's data turns read only.
- Have each one sign the pilot agreement before their first sign in.
- Book a 30 minute onboarding call with each, screen shared. Watch them add
  their first building without helping unless they are stuck. Write down every
  place they hesitate.

### Step 9: collect feedback for 8 weeks

- **Weekly**: read every message from the feedback form within a day and
  answer it. Keep a single list (a Jira board works) of what was asked for and
  how often.
- **Week 2 and week 6**: a 20 minute call with each user. Ask what they did
  last time they used it, what they did outside the app instead, and what
  annoyed them; not whether they like it.
- **Week 8**: a short survey with one decisive question: "How would you feel if
  you could no longer use this product?" (very disappointed / somewhat / not),
  and "What would you pay per month for it?".
- Measure, per user, by looking in the database once a week: did they create a
  building, an apartment, a tenant, an invoice? When did they last sign in?
  That is enough without an analytics tool.

Release fixes during the pilot as described in DEPLOYMENT.md (`git pull`,
`scripts/start-prod.sh`), tagging the commit first (`git tag live-YYYY-MM-DD`)
so there is something to go back to. Deploy in the evening and tell users in
advance if a change is visible.

### Step 10: decide (end of week 10)

With the survey and the list from step 9, decide what part 2 is: payment,
the most requested features, or a change of direction.

## 3. What is missing or easy to forget

### 3.1 Legal, for a service offered in Germany

Not legal advice; a lawyer or a generator such as eRecht24 or
Datenschutz-Generator.de should produce the final texts.

- **Impressum** (§ 5 DDG, which replaced § 5 TMG in 2024). Required even for a
  free pilot offered to the public: name, postal address, email, and a
  company's register entry and VAT id if there are any.
- **Datenschutzerklärung** (privacy policy, Art. 13 GDPR): what is stored, why,
  for how long, who processes it for us (Hetzner, Brevo), and the user's
  rights.
- **We are a processor for our users' tenants' data.** Owners store their
  tenants' names and contact details in the app. For that data the owner is
  the controller and we are their processor, which needs a **data processing
  agreement (AVV, Art. 28 GDPR)** with every owner, including in the pilot. It
  can be a standard template attached to the pilot agreement. It refers to a
  short list of technical and organisational measures (TOMs): HTTPS, EU
  hosting, access by SSH key only, nightly encrypted off-site backups, and so
  on. Encrypt the Storage Box copy, or choose a Storage Box with encryption at
  rest, so that statement holds.
- **Record of processing activities** (Art. 30 GDPR): a one page table we keep,
  not publish.
- **Terms of use for the pilot**: free, provided as is, may change, ends on a
  given date, what happens to the data after it (export on request, deletion
  after 30 days), liability limited as far as German law allows.
- **Not tax advice.** The profit and loss report helps with a tax return; it is
  not bookkeeping in the sense of GoBD and the terms should say so.
- **Cookies.** The app sets only a session cookie and a forgery protection
  token, Keycloak only its own sign in session cookies, and the theme and
  language are kept in the browser at the user's request.
  Those are strictly necessary (§ 25 TDDDG), so no consent banner is needed.
  This stays true only while there is no analytics and no third party script;
  fonts are already served from our own domain. Adding anything else means
  revisiting this.
- **Business registration.** A free pilot run privately is a grey zone; the
  moment money is charged, a Gewerbeanmeldung and a tax number are needed. Ask
  a Steuerberater before part 2.

### 3.2 Product gaps that matter for real users

- **No email at all** (step 2). Besides password reset, email verification is
  off, so a typo in an address at registration goes unnoticed.
- **No brute force protection or password policy** on the realm (step 2).
- **Public registration is open**, with no email verification and a 30 day
  trial for anyone. For the pilot that is acceptable if we watch Accounts
  weekly; otherwise hide the link on the sign in page until part 2.
- **The trial ends silently** and turns the data read only. Extend pilot users
  by hand (step 8).
- **No way for an owner to delete their account or export their data**, both
  rights under GDPR (Art. 17 and 20). For a handful of pilot users, doing it
  by hand on request within a month is acceptable: delete the Keycloak user
  and every row keyed on their id, or hand over a database extract. Write the
  procedure down once, before it is needed.
- **Assistants see buttons they cannot use.** The backend refuses the action,
  but expect it to come up in feedback.
- **Subscriptions run on UTC midnight**, an hour or two off German time. Harmless
  in a pilot.

### 3.3 Operations

- **Support channel.** One email address, written in the Impressum, the
  feedback form and the onboarding call. Promise an answer within one working
  day and keep to it; in a pilot, how we respond is part of what is tested.
- **Someone has to be on call** in an informal way: who looks at an UptimeRobot
  alert on a Sunday, and who has the SSH key and `.env.production` if that
  person is away. Keep a copy of `.env.production` in a password manager;
  losing it means losing the client secret and every password.
- **Rollback** is `git checkout <previous tag>` and `scripts/start-prod.sh`.
  A migration Flyway has already run is not undone by that, so a release with a
  migration needs a fresh backup taken just before it.
- **Incident note.** If data is ever exposed, GDPR gives 72 hours to report it
  to the data protection authority, and we have to tell the owners, who are
  controllers for their tenants' data. Know in advance where the authority's
  form is.

### 3.4 Not now, and when

| Later | When it pays off |
|---|---|
| Online payment (Stripe or similar) | Once the survey says people would pay |
| Warning before a subscription ends | Together with payment |
| Staging server | When more than one person deploys, or before the first paying customer |
| Container registry and image tags | When building on the server becomes the slow or risky part of a release |
| Error tracking (Sentry, GlitchTip) | When the number of users makes reading logs impractical |
| Managed or replicated database | When an hour of downtime costs more than the database |
| Self service account deletion and export | Before opening registration to everyone |
| More languages | When a pilot user asks for one |

## 4. Checklist before the first invitation

- [ ] Name checked against trademark registers, domain bought
- [ ] SMTP configured, reset password email arrives and is not spam (SPF, DKIM)
- [ ] Brute force protection and password policy active on the live realm
- [ ] Impressum and Datenschutzerklärung online and linked from every page
- [ ] Feedback link in the app
- [ ] Server hardened, firewall at 22, 80 and 443 only
- [ ] AVV signed with Hetzner and Brevo
- [ ] Off-site backups running, one restore done and signed in after it
- [ ] Uptime checks and the backup heartbeat alerting a real inbox
- [ ] `.env.production` copied to a password manager
- [ ] Full rehearsal done in English, French and on a phone
- [ ] Pilot agreement with terms of use and AVV ready to sign
- [ ] Deletion and export procedure written down
- [ ] Pilot users' subscriptions extended to the end of the pilot
