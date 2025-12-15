set -e

export PATH="/opt/podman/bin:$PATH"
cd "$(dirname "$0")/.."

echo "=========================================="
echo "сценарий 1: отказ MinIO, БД работает"
echo "=========================================="

echo ""
echo "1. останавливаем minio..."
podman compose stop minio

echo ""
echo "2. пытаемся импортировать файл..."
curl -X POST "http://localhost:8080/is_lab1/import/persons?filename=test.xml&username=test" \
  -H "Content-Type: application/xml" \
  --data-binary "@test-data/persons_import_happy_path.xml" \
  -w "\nHTTP Status: %{http_code}\n" || true

echo ""
echo "3. проверяем историю импорта..."
curl -s "http://localhost:8080/is_lab1/import/history?username=test" | jq '.[0] | {id, username, status, fileName, errorMessage}' || echo "Не удалось получить историю"

echo ""
echo "4. проверяем логи backend (последние ошибки)... (отсмотреть, есть ли ошибки с этим файлом)"
podman compose logs backend 2>&1 | grep -i "minio\|failed\|rollback" | tail -5 || echo "Нет ошибок в логах"

echo ""
echo "5. запускаем minio обратно..."
podman compose start minio

echo ""
echo "=========================================="
echo "ожидаемый результат:"
echo "- файл НЕ был загружен в MinIO "
echo "- данные НЕ сохранены в БД (rollback)"
echo "- в import_history статус FAILED"
echo "=========================================="

