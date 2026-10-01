#!/usr/bin/env bash
# Read the stack's logs, on mac or linux.
#
#   scripts/logs.sh                  follow every service, live
#   scripts/logs.sh backend          follow one service
#   scripts/logs.sh --save           write every service's logs to logs/<date-time>.log
#   scripts/logs.sh backend --save   the same, for one service
#   scripts/logs.sh --viewer         start the web viewer
#   scripts/logs.sh --viewer-off     stop it
#
# scripts/logs-prod.sh runs this against the production stack.
set -euo pipefail

cd "$(dirname "$0")/.."

# Left unquoted where used, so it splits into separate arguments.
COMPOSE_ARGS=${LOGS_COMPOSE_ARGS:-}

compose() {
  # shellcheck disable=SC2086
  docker compose $COMPOSE_ARGS "$@"
}

mode=follow
service=
for arg in "$@"; do
  case "$arg" in
    --save) mode=save ;;
    --viewer) mode=viewer ;;
    --viewer-off) mode=viewer-off ;;
    -*) echo "Unknown option $arg" >&2; exit 1 ;;
    *) service=$arg ;;
  esac
done

case "$mode" in
  save)
    mkdir -p logs
    file="logs/$(date +%Y%m%d-%H%M%S)${service:+-$service}.log"
    compose logs --no-color --timestamps ${service:+"$service"} > "$file"
    echo "Saved to $file"
    ;;
  viewer)
    compose --profile logs up -d logs
    # The port Docker published, which LOGS_PORT in the env file may have moved.
    echo "Logs viewer at http://$(compose --profile logs port logs 8080)"
    ;;
  viewer-off)
    compose --profile logs rm --stop --force logs
    ;;
  follow)
    compose logs --follow --tail 100 ${service:+"$service"}
    ;;
esac
