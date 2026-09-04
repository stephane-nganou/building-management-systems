#!/usr/bin/env bash
# Stop the stack. Pass --wipe to also drop the database volume.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ "${1:-}" = "--wipe" ]; then
  docker compose down -v
  echo "Stack stopped and database volume removed."
else
  docker compose down
  echo "Stack stopped. Data is kept; run with --wipe to remove it."
fi
