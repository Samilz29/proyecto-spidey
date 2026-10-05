#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
(cd frontend && npm ci && npm run build)
mkdir -p backend/src/main/resources/static
cp -R frontend/dist/browser/. backend/src/main/resources/static/
(cd backend && ./mvnw clean verify)
printf '\nReady: java -jar backend/target/gymlog-1.0.0.jar\nOpen http://127.0.0.1:8080\n'
