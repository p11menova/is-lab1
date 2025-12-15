#!/bin/bash
# Сценарий 3: Ошибка в бизнес-логике (оба работают)

set -e

export PATH="/opt/podman/bin:$PATH"
cd "$(dirname "$0")/.."

echo "=========================================="
echo "сценарий 3: ошибка в бизнес-логике"
echo "=========================================="

echo ""
echo "1. проверяем, что оба сервиса работают..."
podman compose ps postgres minio | grep -q "Up" && echo "✓ Оба сервиса работают" || echo "✗ Один из сервисов не работает"

echo ""
echo "2. пытаемся импортировать файл с невалидными данными..."
curl -X POST "http://localhost:8080/is_lab1/import/persons?username=test&filename=invalid.xml" \
  -H "Content-Type: application/xml" \
  --data-binary "@test-data/persons_import_invalid_xml.xml" \
  -w "\nHTTP Status: %{http_code}\n" || true

echo ""
echo "3. проверяем историю импорта..."
curl -s "http://localhost:8080/is_lab1/import/history?username=test" | jq '.[0] | {id, username, status, fileName, errorMessage}' || echo "Не удалось получить историю"

echo ""
echo "4. проверяем MinIO (файл НЕ должен остаться)..."
podman compose exec minio mc ls myminio/import-files/ 2>&1 || echo "MinIO недоступен"

echo ""
echo "5. проверяем логи backend (последние ошибки)..."
podman compose logs backend 2>&1 | grep -i "validation\|error\|rollback\|transaction" | tail -10 || echo "Нет логов"

echo ""
echo "=========================================="
echo "ожидаемый результат:"
echo "- ошибка валидации в бизнес-логике"
echo "- данные НЕ сохранены в БД (rollback)"
echo "- файл не загружен в MinIO (rollback)"
echo "=========================================="

