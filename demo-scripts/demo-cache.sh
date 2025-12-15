#!/bin/bash

set -e

export PATH="/opt/podman/bin:$PATH"
cd "$(dirname "$0")/.."

API_BASE="http://localhost:8080/is_lab1"

echo "=========================================="
echo "демонстрация L2 JPA Cache (Ehcache)"
echo "=========================================="

echo ""
echo "1. проверяем статус логирования статистики кэша..."
STATUS=$(curl -s "${API_BASE}/cache-statistics/enabled" 2>&1)
echo "$STATUS"

echo ""
echo "2. включаем логирование статистики кэша..."
curl -s -X POST "${API_BASE}/cache-statistics/enable" 2>&1
echo ""

echo ""
echo "3. первый запрос - данные из БД (cache miss)..."
echo "запрос: GET ${API_BASE}/movies/1"
TIME1=$(curl -s -o /dev/null -w "%{time_total}" "${API_BASE}/movies/1" 2>&1)
RESPONSE1=$(curl -s "${API_BASE}/movies/1" 2>&1 | head -1)
echo "время обработки: ${TIME1} секунд"
echo "ответ: ${RESPONSE1}"
echo ""

echo ""
echo "4. второй запрос - данные из кэша (cache hit)..."
echo "запрос: GET ${API_BASE}/movies/1"
TIME2=$(curl -s -o /dev/null -w "%{time_total}" "${API_BASE}/movies/1" 2>&1)
RESPONSE2=$(curl -s "${API_BASE}/movies/1" 2>&1 | head -1)
echo "время обработки: ${TIME2} секунд"
echo "ответ: ${RESPONSE2}"
echo ""

echo ""
echo "5. третий запрос - данные из кэша (cache hit)..."
echo "запрос: GET ${API_BASE}/movies/1"
TIME3=$(curl -s -o /dev/null -w "%{time_total}" "${API_BASE}/movies/1" 2>&1)
RESPONSE3=$(curl -s "${API_BASE}/movies/1" 2>&1 | head -1)
echo "время обработки: ${TIME3} секунд"
echo "ответ: ${RESPONSE3}"
echo ""

echo ""
echo "сравнение времени:"
echo "  первый запрос (БД):  ${TIME1} сек"
echo "  второй запрос (кэш): ${TIME2} сек"
echo "  третий запрос (кэш): ${TIME3} сек"
if command -v bc >/dev/null 2>&1; then
    COMPARE=$(echo "$TIME2 < $TIME1" | bc -l)
    if [ "$COMPARE" = "1" ]; then
        SPEEDUP=$(echo "scale=2; $TIME1 / $TIME2" | bc)
        echo "  ускорение: ~${SPEEDUP}x быстрее (кэш vs БД)"
    fi
fi
echo ""

echo ""
echo "6. проверяем логи backend (статистика кэша)..."
podman compose logs backend 2>&1 | grep -i "CacheStatisticsInterceptor\|Cache Statistics\|L2 Hits\|L2 Misses" | tail -10 || echo "Нет логов статистики (проверьте, что interceptor работает)"

echo ""
echo "7. отключаем логирование статистики кэша..."
curl -s -X POST "${API_BASE}/cache-statistics/disable" 2>&1 | head -1
echo ""

echo ""
echo "8. делаем еще один запрос (логирование отключено)..."
TIME4=$(curl -s -o /dev/null -w "%{time_total}" "${API_BASE}/movies/1" 2>&1)
RESPONSE4=$(curl -s "${API_BASE}/movies/1" 2>&1 | head -1)
echo "время обработки: ${TIME4} секунд"
echo "ответ: ${RESPONSE4}"
echo ""

echo ""
echo "9. проверяем логи (не должно быть статистики)..."
podman compose logs backend 2>&1 | grep -i "Cache Statistics" | tail -5 || echo "Логирование отключено - статистики нет"

echo ""
echo "=========================================="
echo "ожидаемый результат:"
echo "- первый запрос: медленнее (данные из БД, cache miss)"
echo "- второй и третий запросы: быстрее (данные из кэша, cache hit)"
echo "- при включенном логировании видны логи Cache Statistics"
echo "- при отключенном логировании логов нет"
echo "- ускорение при использовании кэша обычно 2-10x"
echo "=========================================="

