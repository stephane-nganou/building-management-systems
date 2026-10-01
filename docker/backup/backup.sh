#!/bin/sh
# Dumps the application's database and Keycloak's once on start and then every
# night at 02:00 UTC into /backups, and deletes dumps older than BACKUP_KEEP_DAYS.
# Copying /backups off the server is the operator's job. To restore, see
# docs/DEPLOYMENT.md.
set -eu

dump() {
  stamp=$(date -u +%Y-%m-%dT%H%M)
  for db in "$APP_DB" keycloak; do
    # Written aside and moved into place, so a half written dump never looks complete.
    pg_dump --format=custom --file="/backups/.$db-$stamp.partial" "$db"
    mv "/backups/.$db-$stamp.partial" "/backups/$db-$stamp.dump"
  done
  find /backups -name '*.dump' -mtime +"$BACKUP_KEEP_DAYS" -delete
  echo "Backed up $APP_DB and keycloak at $stamp"
}

until pg_isready --quiet; do
  sleep 2
done

dump
while true; do
  now=$(date -u +%s)
  sleep $(( (86400 + 7200 - now % 86400 - 1) % 86400 + 1 ))
  dump
done
