#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${DOCKER_DIR}"

echo "==> Остановка и удаление контейнеров..."
docker compose down

echo "Готово. Данные PostgreSQL сохранены в volume cian_pgdata."
echo "Полный сброс БД: ./docker/scripts/reset-db.sh"
