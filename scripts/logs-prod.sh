#!/usr/bin/env bash
# Read the production stack's logs, on the server. Takes the same arguments as
# scripts/logs.sh. The viewer answers on the server's localhost only, so reach
# it over an SSH tunnel; see docs/DEPLOYMENT.md.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env.production ]; then
  echo "No .env.production. Run this from the production checkout." >&2
  exit 1
fi

LOGS_COMPOSE_ARGS="-f docker-compose.prod.yml --env-file .env.production" exec scripts/logs.sh "$@"
