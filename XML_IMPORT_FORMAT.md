# Формат XML для импорта

## Структура файлов

### Фильмы (movies_import.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<movies>
    <movie>
        <name>Название фильма</name>
        <genre>ACTION</genre>
        <mpaaRating>PG_13</mpaaRating>
        <oscarsCount>5</oscarsCount>
        <budget>1000000.0</budget>
        <totalBoxOffice>5000000</totalBoxOffice>
        <length>120</length>
        <goldenPalmCount>1</goldenPalmCount>
        <coordinates>
            <x>10</x>
            <y>20</y>
        </coordinates>
        <operator>
            <name>Имя оператора</name>
            <hairColor>BLACK</hairColor>
            <eyeColor>GREEN</eyeColor>
            <location>
                <x>1</x>
                <y>2.5</y>
                <z>3.0</z>
            </location>
            <birthday>1980-01-15T00:00:00</birthday>
            <nationality>CHINA</nationality>
        </operator>
        <director>
            <!-- Аналогично operator -->
        </director>
        <screenwriter>
            <!-- Аналогично operator -->
        </screenwriter>
    </movie>
</movies>
```

### Персоны (persons_import.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persons>
    <person>
        <name>Имя персоны</name>
        <hairColor>BLACK</hairColor>
        <eyeColor>GREEN</eyeColor>
        <location>
            <x>1</x>
            <y>2.5</y>
            <z>3.0</z>
        </location>
        <birthday>1980-01-15T00:00:00</birthday>
        <nationality>CHINA</nationality>
    </person>
</persons>
```

## Тестовые файлы

В папке `test-data/` находятся примеры XML файлов:
- `movies_import.xml` - пример импорта фильмов
- `persons_import.xml` - пример импорта персон

## Важно

1. Все обязательные поля должны быть заполнены
2. Для фильма обязателен оператор (operator)
3. Проверяется уникальность:
   - Movie: (name, operator.id, director.id)
   - Person: (name, birthday)
4. При ошибке вся транзакция откатывается

