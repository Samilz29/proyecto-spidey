#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
(cd backend && ./mvnw package)
(cd frontend && npm ci && npx playwright install chromium && npm run test:e2e)
