#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${DOCKER_DIR}"

echo "Сборка образов и запуск контейнеров..."
docker compose up --build -d

echo ""
echo "Приложение:         http://localhost:8080"
echo "Swagger UI:         http://localhost:8080/swagger-ui/index.html"
echo "RabbitMQ UI:        http://localhost:15672 (cian / cian)"
echo "Mailpit UI:         http://localhost:8025"
echo "Админ:              admin@cian.local / admin123"
echo ""
echo "Остановка:          ./docker/scripts/down.sh"
