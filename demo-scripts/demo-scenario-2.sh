set -e

export PATH="/opt/podman/bin:$PATH"
cd "$(dirname "$0")/.."

echo "=========================================="
echo "сценарий 2: отказ БД, MinIO работает"
echo "=========================================="

echo ""
echo "1. останавливаем postgres..."
podman compose stop postgres

echo ""
echo "2. пытаемся импортировать файл..."
curl -X POST "http://localhost:8080/is_lab1/import/persons?username=test&filename=test.xml" \
  -H "Content-Type: application/xml" \
  --data-binary "@test-data/persons_import_happy_path.xml" \
  -w "\nHTTP Status: %{http_code}\n" || true

echo ""
echo "3. проверяем MinIO (файл мог быть загружен в prepare phase)..."
podman compose exec minio mc ls myminio/import-files/ 2>&1 || echo "MinIO недоступен"

echo ""
echo "4. проверяем логи backend (последние ошибки)..."
podman compose logs backend 2>&1 | grep -i "postgres\|database\|rollback\|transaction" | tail -10 || echo "Нет логов"

echo ""
echo "5. проверяем MinIO еще раз (файл должен быть удален после rollback)..."
sleep 3
podman compose exec minio mc ls myminio/import-files/ 2>&1 || echo "MinIO недоступен"

echo ""
echo "6. запускаем postgres обратно..."
podman compose start postgres

echo ""
echo "=========================================="
echo "ожидаемый результат:"
echo "- файл загружен в MinIO (prepare phase)"
echo "- ошибка при сохранении в БД (БД недоступна)"
echo "- файл удален из MinIO (rollback)"
echo "=========================================="

