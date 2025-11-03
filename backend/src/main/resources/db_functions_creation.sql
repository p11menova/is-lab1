-- Функции для MovieRepository: fn_count_by_mpaa, fn_count_genre_gt,
-- fn_movies_genre_lt, fn_movies_zero_oscars, fn_operators_zero_oscars
-- Адаптируйте имена таблиц/полей под вашу схему, если нужно.

-- 1) Подсчёт по MPAA: возвращает пару (mpaa, count)
CREATE OR REPLACE FUNCTION fn_count_by_mpaa()
    RETURNS TABLE(mpaa text, cnt bigint)
    LANGUAGE sql
AS $$
SELECT COALESCE(mpaa_rating::text, 'UNKNOWN') AS mpaa, COUNT(*)::bigint
FROM movies
GROUP BY COALESCE(mpaa_rating::text, 'UNKNOWN')
ORDER BY mpaa;
$$;

-- 2) Количество фильмов с genre > threshold (lexicographic/enum text)
CREATE OR REPLACE FUNCTION fn_count_genre_gt(threshold text)
    RETURNS bigint
    LANGUAGE sql
AS $$
SELECT COUNT(*)::bigint
FROM movies
WHERE (genre::text) > threshold;
$$;

-- 3) Список фильмов с genre < threshold
-- Возвращает строки таблицы movie (SETOF movie)
CREATE OR REPLACE FUNCTION fn_movies_genre_lt(threshold text)
    RETURNS SETOF movies
    LANGUAGE sql
AS $$
SELECT *
FROM movies
WHERE (genre::text) < threshold
ORDER BY creation_date DESC;
$$;

-- 4) Фильмы с нулём oscars_count
CREATE OR REPLACE FUNCTION fn_movies_zero_oscars()
    RETURNS SETOF movies
    LANGUAGE sql
AS $$
SELECT *
FROM movies
WHERE COALESCE(oscars_count,0) = 0
ORDER BY creation_date DESC;
$$;

-- 5) Операторы (persons), у которых все фильмы имеют 0 oscar'ов
-- (Берём операторов, которые связаны с какими-либо фильмами, и исключаем тех,
-- у кого есть фильм с oscars_count > 0)
CREATE OR REPLACE FUNCTION fn_operators_zero_oscars()
    RETURNS SETOF persons
    LANGUAGE sql
AS $$
SELECT p.*
FROM persons p
WHERE p.id IN (SELECT DISTINCT m.operator_id FROM movies m WHERE m.operator_id IS NOT NULL)
  AND p.id NOT IN (SELECT DISTINCT m.operator_id FROM movies m WHERE COALESCE(m.oscars_count,0) > 0);
$$;