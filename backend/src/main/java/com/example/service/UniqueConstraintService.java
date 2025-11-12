package com.example.service;

import com.example.models.Movie;
import com.example.models.Person;
import com.example.repository.MovieRepository;
import com.example.repository.PersonRepository;
import com.example.validators.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UniqueConstraintService {

    @Inject private MovieRepository movieRepository;

    @Inject private PersonRepository personRepository;

    public void validateMovieUniqueness(Movie movie) throws ValidationException {
        if (movie.getOperator() == null) {
            throw new ValidationException("Operator is required for uniqueness check.");
        }

        Long operatorId = movie.getOperator().getId();
        if (operatorId == null) {
            throw new ValidationException("Operator ID is required for uniqueness check.");
        }

        Long directorId = movie.getDirector() != null ? movie.getDirector().getId() : null;

        boolean exists;
        if (movie.getId() != null) {
            exists =
                    movieRepository.existsByNameAndOperatorAndDirectorExcludingId(
                            movie.getName(), operatorId, directorId, movie.getId());
        } else {
            exists =
                    movieRepository.existsByNameAndOperatorAndDirector(
                            movie.getName(), operatorId, directorId);
        }

        if (exists) {
            throw new ValidationException(
                    "Movie with name '"
                            + movie.getName()
                            + "' already exists with the same operator and director.");
        }
    }

    public void validatePersonUniqueness(Person person) throws ValidationException {
        boolean exists;
        if (person.getId() != null) {
            exists =
                    personRepository.existsByNameAndBirthdayExcludingId(
                            person.getName(), person.getBirthday(), person.getId());
        } else {
            exists =
                    personRepository.existsByNameAndBirthday(
                            person.getName(), person.getBirthday());
        }

        if (exists) {
            throw new ValidationException(
                    "Person with name '"
                            + person.getName()
                            + "' and birthday '"
                            + person.getBirthday()
                            + "' already exists.");
        }
    }
}
