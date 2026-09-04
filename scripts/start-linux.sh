#!/usr/bin/env bash
# Start the whole stack: Postgres, Keycloak, backend and frontend.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "Creating .env from .env.example"
  cp .env.example .env
fi

docker compose up --build -d

echo
echo "Frontend   http://localhost:${FRONTEND_PORT:-4200}"
echo "Backend    http://localhost:${BACKEND_PORT:-8080}"
echo "API docs   http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html"
echo "Keycloak   http://localhost:${KEYCLOAK_PORT:-8081}  (admin / admin)"
echo
echo "Sign in as owner / owner, or assistant / assistant."
echo "Keycloak takes about a minute on the first run while it imports the realm."
