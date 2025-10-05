package com.example.repository;

import com.example.models.Movie;
import com.example.models.Person;
import com.example.models.enums.MovieGenre;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
}
