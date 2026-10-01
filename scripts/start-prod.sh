#!/usr/bin/env bash
# Start the production stack on a Linux server. See docs/DEPLOYMENT.md.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env.production ]; then
  echo "No .env.production. Copy .env.production.example to it and fill in every value." >&2
  exit 1
fi

docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
docker compose -f docker-compose.prod.yml --env-file .env.production ps
