#!/usr/bin/env bash
# Stop the production stack, the logs viewer included. Data, certificates and
# backups are kept.
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose -f docker-compose.prod.yml --env-file .env.production --profile logs down
