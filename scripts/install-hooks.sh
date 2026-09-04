#!/usr/bin/env bash
# Point git at the hooks kept in the repository, so everyone runs the same ones.
set -euo pipefail
cd "$(dirname "$0")/.."
git config core.hooksPath .githooks
echo "Hooks installed. The end to end suite now runs before every push."
