# Deploying to production

`docker-compose.prod.yml` runs the whole application on one Linux server behind
[Caddy](https://caddyserver.com), which obtains and renews an HTTPS certificate
from Let's Encrypt. Everything is served from one domain:

| Path | Goes to |
|---|---|
| `/api/*` | the backend |
| `/auth/*` | Keycloak: the sign in page and the token endpoints |
| `/auth/admin*`, `/auth/realms/master*` | refused with 404 |
| anything else | the frontend |

Only ports 80 and 443 are open to the internet. Postgres, the backend and the
frontend publish nothing; Keycloak's admin console listens on
`127.0.0.1:8081` alone.

## First install

You need a Linux server with Docker and the Compose plugin, a domain whose DNS
`A` (and `AAAA`) record points at the server, ports 80 and 443 open, and an SMTP
relay (Brevo's free plan will do) for the emails Keycloak sends: the link that
confirms a new owner's address, and password resets. The stack refuses to start
without the `BMS_SMTP_*` values. Send a test from the relay first, and check the
domain's SPF and DKIM records, or those emails land in spam.

```bash
git clone https://github.com/stephane-nganou/building-management-systems.git
cd building-management-systems
cp .env.production.example .env.production
# Fill in every value. For each password and the client secret:
openssl rand -base64 32
scripts/start-prod.sh
```

The first start builds the images and takes a few minutes. When
`scripts/start-prod.sh` lists every service as running, open
`https://<BMS_DOMAIN>` and sign in as `BMS_ADMIN_USERNAME` with
`BMS_ADMIN_PASSWORD`. Keycloak asks for a new password straight away; the one in
`.env.production` works only once. From the **Accounts** screen, sign owners up.

A new realm has no other user: the demo accounts of the development stack
(`docker/keycloak/users-demo.json`) never reach production.

## Updating

```bash
git pull
scripts/start-prod.sh
```

Flyway migrates the database when the backend starts, and `keycloak-sync`
applies any change to `docker/keycloak/realm-bms.json`. Users are never
replaced.

## Keycloak's admin console

It is not on the internet. Open an SSH tunnel and browse to it locally:

```bash
ssh -L 8081:localhost:8081 <user>@<server>
# then http://localhost:8081/auth/admin, as KEYCLOAK_ADMIN / KEYCLOAK_ADMIN_PASSWORD
```

## Backups

The `backup` service dumps the application database (`bms`) and Keycloak's
(`keycloak`) when it starts and every night at 02:00 UTC, into `backups/` next
to the compose file, and deletes dumps older than `BACKUP_KEEP_DAYS` (14).
Those files are on the same disk as the database: copy them somewhere else,
for example with a nightly `rsync` or `rclone` from another machine.

To restore, stop what writes to the databases, restore both dumps from the same
moment, and start again:

```bash
C="docker compose -f docker-compose.prod.yml --env-file .env.production"
$C stop backend keycloak
$C exec -T postgres pg_restore --clean --if-exists -U bms -d bms      < backups/bms-<stamp>.dump
$C exec -T postgres pg_restore --clean --if-exists -U bms -d keycloak < backups/keycloak-<stamp>.dump
$C start keycloak backend
```

Use `-U` with your `POSTGRES_USER` if you changed it. Restore both, always:
the application keys every record on the Keycloak user's id, so one without
the other leaves accounts that cannot find their buildings.

## Stopping

```bash
scripts/stop-prod.sh
```

Data, certificates and backups are kept. `docker compose -f docker-compose.prod.yml down -v`
would delete the database and the certificates; do not run it on a live server.
