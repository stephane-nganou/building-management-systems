#!/usr/bin/env bash
# Run the end to end suite against a throwaway copy of the whole stack.
#
# It uses its own compose project, so the database it creates is not the one you
# develop against. It does use the same ports, because the realm's redirect URIs
# name them, so stop the development stack first.
set -euo pipefail

cd "$(dirname "$0")/.."

PROJECT=bms-e2e
KEEP_STACK=${E2E_KEEP_STACK:-0}

cleanup() {
  if [ "$KEEP_STACK" = "1" ]; then
    echo "Leaving the $PROJECT stack up, because E2E_KEEP_STACK=1."
  else
    docker compose -p "$PROJECT" down -v >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

# Waits for a URL to answer at all. The stack is only useful once every part is
# up: starting the tests a second too early is a flake, not a failure.
wait_for() {
  local name=$1 url=$2 attempts=${3:-90}
  printf 'Waiting for %s' "$name"
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS -o /dev/null "$url" 2>/dev/null; then
      printf ' ready\n'
      return 0
    fi
    printf '.'
    sleep 2
  done
  printf '\n%s never became ready at %s\n' "$name" "$url" >&2
  docker compose -p "$PROJECT" logs --tail 50 >&2
  return 1
}

# The compose file pins container names and the realm pins the ports, so the
# development stack and this one cannot both be up. Say so plainly, rather than
# letting docker report a name conflict.
conflicting=$(docker ps --format '{{.Names}}	{{.Label "com.docker.compose.project"}}'   | awk -F'	' -v project="$PROJECT" '$1 ~ /^bms-/ && $2 != project { printf "%s ", $1 }')
if [ -n "$conflicting" ]; then
  echo "Another stack is already running: ${conflicting% }" >&2
  echo "It holds the same container names and ports. Stop it first:" >&2
  echo "  scripts/stop-linux.sh    (or stop-mac.sh, or scripts/stop-windows.ps1)" >&2
  exit 1
fi

echo "Starting the $PROJECT stack."
docker compose -p "$PROJECT" up --build -d

wait_for Keycloak http://localhost:8081/realms/bms/.well-known/openid-configuration
wait_for backend http://localhost:8080/actuator/health
wait_for frontend http://localhost:4200/

cd frontend
[ -d node_modules ] || npm ci
npx playwright install chromium >/dev/null
npx playwright test "$@"
