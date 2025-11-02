# Тестовые данные для импорта

## Формат файлов

Файлы должны быть в формате XML с следующей структурой:

### Для фильмов (movies_import.xml):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<movies>
    <movie>
        <name>Название фильма</name>
        <genre>ACTION|WESTERN|ADVENTURE|THRILLER|HORROR</genre>
        <mpaaRating>G|PG|PG_13|NC_17</mpaaRating>
        <oscarsCount>число</oscarsCount>
        <budget>число</budget>
        <totalBoxOffice>число (опционально)</totalBoxOffice>
        <length>число (опционально)</length>
        <goldenPalmCount>число</goldenPalmCount>
        <coordinates>
            <x>число</x>
            <y>число</y>
        </coordinates>
        <operator>
            <name>Имя оператора</name>
            <hairColor>GREEN|RED|BLACK|YELLOW|ORANGE</hairColor>
            <eyeColor>GREEN|RED|BLACK|YELLOW|ORANGE (опционально)</eyeColor>
            <location>
                <x>число</x>
                <y>число</y>
                <z>число</z>
            </location>
            <birthday>YYYY-MM-DDTHH:mm:ss (опционально)</birthday>
            <nationality>CHINA|VATICAN|NORTH_KOREA (опционально)</nationality>
        </operator>
        <director>
            <!-- аналогично operator (опционально) -->
        </director>
        <screenwriter>
            <!-- аналогично operator (опционально) -->
        </screenwriter>
    </movie>
</movies>
```

### Для персон (persons_import.xml):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<persons>
    <person>
        <name>Имя персоны</name>
        <hairColor>GREEN|RED|BLACK|YELLOW|ORANGE</hairColor>
        <eyeColor>GREEN|RED|BLACK|YELLOW|ORANGE (опционально)</eyeColor>
        <location>
            <x>число</x>
            <y>число</y>
            <z>число</z>
        </location>
        <birthday>YYYY-MM-DDTHH:mm:ss (опционально)</birthday>
        <nationality>CHINA|VATICAN|NORTH_KOREA (опционально)</nationality>
    </person>
</persons>
```

## Использование

1. Откройте фронтенд
2. Перейдите на вкладку "Import"
3. Выберите соответствующий XML файл
4. Нажмите на кнопку загрузки
5. Проверьте историю импорта для статуса операции

## Важные замечания

- Все поля, отмеченные как обязательные в модели, должны присутствовать
- Оператор для фильма обязателен
- При импорте проверяется уникальность: Movie (name, operator.id, director.id), Person (name, birthday)
- При ошибке валидации вся транзакция откатывается

