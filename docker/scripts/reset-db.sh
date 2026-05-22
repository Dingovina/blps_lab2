#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${DOCKER_DIR}"

echo "==> Остановка контейнеров и удаление volume с данными БД..."
docker compose down -v

echo "==> Повторный запуск с чистой БД..."
docker compose up --build -d

echo "Готово. База пересоздана из src/sql/schema.sql и seed.sql."
