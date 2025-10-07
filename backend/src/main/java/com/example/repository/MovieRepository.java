package com.example.repository;

import com.example.models.Movie;
import com.example.models.Person;
import com.example.models.enums.MovieGenre;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class MovieRepository {

    @PersistenceContext(unitName = "my-pu")
    private EntityManager em;

    @Transactional
    public Movie saveOrUpdate(Movie movie) {
        return em.merge(movie);
    }

    public Optional<Movie> findById(Long id) {
        return Optional.ofNullable(em.find(Movie.class, id));
    }

    public List<Movie> findAll() {
        return em.createQuery("SELECT m FROM Movie m ORDER BY m.creationDate DESC", Movie.class)
                .getResultList();
    }

    public List<Movie> findPagedFilteredSorted(
            Integer page,
            Integer size,
            String sortBy,
            String sortOrder,
            String nameFilter,
            String genreFilter,
            String mpaaFilter,
            String operatorNameFilter,
            String directorNameFilter,
            String screenwriterNameFilter) {

        StringBuilder jpql = new StringBuilder("SELECT m FROM Movie m ");
        boolean needJoinOperator = operatorNameFilter != null && !operatorNameFilter.isBlank();
        boolean needJoinDirector = directorNameFilter != null && !directorNameFilter.isBlank();
        boolean needJoinScreenwriter =
                screenwriterNameFilter != null && !screenwriterNameFilter.isBlank();

        if (needJoinOperator) {
            jpql.append(" LEFT JOIN m.operator op ");
        }
        if (needJoinDirector) {
            jpql.append(" LEFT JOIN m.director dir ");
        }
        if (needJoinScreenwriter) {
            jpql.append(" LEFT JOIN m.screenwriter scr ");
        }

        String where = "";
        if (nameFilter != null && !nameFilter.isBlank()) {
            where += (where.isEmpty() ? " WHERE " : " AND ") + " LOWER(m.name) LIKE LOWER(:name) ";
        }
        if (genreFilter != null && !genreFilter.isBlank()) {
            where +=
                    (where.isEmpty() ? " WHERE " : " AND ")
                            + " LOWER(CAST(m.genre as string)) LIKE LOWER(:genre) ";
        }
        if (mpaaFilter != null && !mpaaFilter.isBlank()) {
            where +=
                    (where.isEmpty() ? " WHERE " : " AND ")
                            + " LOWER(CAST(m.mpaaRating as string)) LIKE LOWER(:mpaa) ";
        }
        if (needJoinOperator) {
            where +=
                    (where.isEmpty() ? " WHERE " : " AND ")
                            + " LOWER(op.name) LIKE LOWER(:opName) ";
        }
        if (needJoinDirector) {
            where +=
                    (where.isEmpty() ? " WHERE " : " AND ")
                            + " LOWER(dir.name) LIKE LOWER(:dirName) ";
        }
        if (needJoinScreenwriter) {
            where +=
                    (where.isEmpty() ? " WHERE " : " AND ")
                            + " LOWER(scr.name) LIKE LOWER(:scrName) ";
        }

        jpql.append(where);

        String safeSortBy = (sortBy == null || sortBy.isBlank()) ? "creationDate" : sortBy;
        String safeSortOrder =
                (sortOrder != null && sortOrder.equalsIgnoreCase("asc")) ? "ASC" : "DESC";
        jpql.append(" ORDER BY m." + safeSortBy + " " + safeSortOrder);

        TypedQuery<Movie> query = em.createQuery(jpql.toString(), Movie.class);

        if (nameFilter != null && !nameFilter.isBlank()) {
            query.setParameter("name", "%" + nameFilter + "%");
        }
        if (genreFilter != null && !genreFilter.isBlank()) {
            query.setParameter("genre", "%" + genreFilter + "%");
        }
        if (mpaaFilter != null && !mpaaFilter.isBlank()) {
            query.setParameter("mpaa", "%" + mpaaFilter + "%");
        }
        if (needJoinOperator) {
            query.setParameter("opName", "%" + operatorNameFilter + "%");
        }
        if (needJoinDirector) {
            query.setParameter("dirName", "%" + directorNameFilter + "%");
        }
        if (needJoinScreenwriter) {
            query.setParameter("scrName", "%" + screenwriterNameFilter + "%");
        }

        if (page != null && size != null && page >= 0 && size > 0) {
            query.setFirstResult(page * size);
            query.setMaxResults(size);
        }

        return query.getResultList();
    }

    @Transactional
    public void delete(Movie movie) {

        em.remove(em.contains(movie) ? movie : em.merge(movie));
    }

    @Transactional
    public void deleteById(Long id) {
        findById(id).ifPresent(this::delete);
    }

    public Map<String, Long> countMoviesByMpaaRating() {

        List<Object[]> results =
                em.createQuery(
                                "SELECT m.mpaaRating, COUNT(m) FROM Movie m GROUP BY m.mpaaRating",
                                Object[].class)
                        .getResultList();

        return results.stream()
                .collect(Collectors.toMap(row -> ((Enum<?>) row[0]).name(), row -> (Long) row[1]));
    }

    public Long countMoviesByGenreGreaterThan(MovieGenre genreThreshold) {

        TypedQuery<Long> query =
                em.createQuery(
                        "SELECT COUNT(m) FROM Movie m WHERE m.genre > :thresholdGenre", Long.class);

        query.setParameter("thresholdGenre", genreThreshold);

        return query.getSingleResult();
    }

    public List<Movie> findMoviesByGenreLessThan(MovieGenre genreThreshold) {
        TypedQuery<Movie> query =
                em.createQuery(
                        "SELECT m FROM Movie m WHERE m.genre < :thresholdGenre", Movie.class);

        query.setParameter("thresholdGenre", genreThreshold);

        return query.getResultList();
    }

    public List<Movie> findMoviesWithZeroOscars() {

        return em.createQuery("SELECT m FROM Movie m WHERE m.oscarsCount = 0", Movie.class)
                .getResultList();
    }

    public List<Person> findOperatorsWhoseMoviesHaveZeroOscars() {

        String subquery = "SELECT DISTINCT m.operator.id FROM Movie m WHERE m.oscarsCount > 0";

        return em.createQuery(
                        "SELECT p FROM Person p WHERE p IN (SELECT m.operator FROM Movie m) AND"
                                + " p.id NOT IN ("
                                + subquery
                                + ")",
                        Person.class)
                .getResultList();
    }

    public List<Object[]> fnCountByMpaaRating() {
        try {
            Query q = em.createNativeQuery("SELECT * FROM fn_count_by_mpaa()");
            @SuppressWarnings("unchecked")
            List<Object[]> rows = q.getResultList();
            return rows;
        } catch (Exception e) {

            return countMoviesByMpaaRating().entrySet().stream()
                    .map(entry -> new Object[] {entry.getKey(), entry.getValue()})
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    public Long fnCountGenreGreaterThan(String threshold) {
        try {
            Query q = em.createNativeQuery("SELECT fn_count_genre_gt(?1)");
            q.setParameter(1, threshold);
            Number n = (Number) q.getSingleResult();
            return n.longValue();
        } catch (Exception e) {

            return countMoviesByGenreGreaterThan(
                    com.example.models.enums.MovieGenre.valueOf(threshold));
        }
    }

    public List<Movie> fnMoviesGenreLessThan(String threshold) {
        try {
            Query q = em.createNativeQuery("SELECT * FROM fn_movies_genre_lt(?1)", Movie.class);
            q.setParameter(1, threshold);
            @SuppressWarnings("unchecked")
            List<Movie> rows = q.getResultList();
            return rows;
        } catch (Exception e) {

            return findMoviesByGenreLessThan(
                    com.example.models.enums.MovieGenre.valueOf(threshold));
        }
    }

    public List<Movie> fnMoviesZeroOscars() {
        try {
            Query q = em.createNativeQuery("SELECT * FROM fn_movies_zero_oscars()", Movie.class);
            @SuppressWarnings("unchecked")
            List<Movie> rows = q.getResultList();
            return rows;
        } catch (Exception e) {

            return findMoviesWithZeroOscars();
        }
    }

    public List<Person> fnOperatorsWithZeroOscars() {
        try {
            Query q =
                    em.createNativeQuery("SELECT * FROM fn_operators_zero_oscars()", Person.class);
            @SuppressWarnings("unchecked")
            List<Person> rows = q.getResultList();
            return rows;
        } catch (Exception e) {

            return findOperatorsWhoseMoviesHaveZeroOscars();
        }
    }
}
