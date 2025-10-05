package com.example.repository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DbFunctionsInitializer {

    @PersistenceContext(unitName = "my-pu")
    private EntityManager em;

    @PostConstruct
    @Transactional
    public void init() {
        // Create SQL functions if not exist
        // Note: requires adequate privileges
        em.createNativeQuery(
                        "CREATE OR REPLACE FUNCTION fn_count_by_mpaa()\n" +
                        "RETURNS TABLE(mpaa text, cnt bigint) AS $$\n" +
                        "SELECT m.mpaa_rating::text, COUNT(*) FROM movies m GROUP BY m.mpaa_rating$$ LANGUAGE SQL;"
                )
                .executeUpdate();

        em.createNativeQuery(
                        "CREATE OR REPLACE FUNCTION fn_count_genre_gt(threshold text)\n" +
                        "RETURNS bigint AS $$\n" +
                        "SELECT COUNT(*) FROM movies m WHERE m.genre > threshold::text::varchar$$ LANGUAGE SQL;"
                )
                .executeUpdate();

        em.createNativeQuery(
                        "CREATE OR REPLACE FUNCTION fn_movies_genre_lt(threshold text)\n" +
                        "RETURNS SETOF movies AS $$\n" +
                        "SELECT * FROM movies m WHERE m.genre < threshold::text::varchar$$ LANGUAGE SQL;"
                )
                .executeUpdate();

        em.createNativeQuery(
                        "CREATE OR REPLACE FUNCTION fn_movies_zero_oscars()\n" +
                        "RETURNS SETOF movies AS $$\n" +
                        "SELECT * FROM movies m WHERE m.oscars_count = 0$$ LANGUAGE SQL;"
                )
                .executeUpdate();

        em.createNativeQuery(
                        "CREATE OR REPLACE FUNCTION fn_operators_zero_oscars()\n" +
                        "RETURNS SETOF persons AS $$\n" +
                        "SELECT DISTINCT p.* FROM persons p\n" +
                        "JOIN movies m ON m.operator_id = p.id\n" +
                        "GROUP BY p.id\n" +
                        "HAVING SUM(CASE WHEN m.oscars_count > 0 THEN 1 ELSE 0 END) = 0$$ LANGUAGE SQL;"
                )
                .executeUpdate();
    }
}


