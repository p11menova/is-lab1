package com.example.validators;

import com.example.models.Coordinates;
import com.example.models.Movie;
import com.example.validators.exceptions.ValidationException;

public class MovieValidator {

    public static void validate(Movie movie) throws ValidationException {
        if (movie == null) {
            throw new ValidationException("Объект Movie не может быть null.");
        }

        if (movie.getName() == null || movie.getName().trim().isEmpty()) {
            throw new ValidationException("Поле 'name' не может быть пустым.");
        }

        Coordinates coordinates = movie.getCoordinates();
        if (coordinates == null) {
            throw new ValidationException("Поле 'coordinates' не может быть null.");
        }

        if (movie.getOscarsCount() == null) {
            throw new ValidationException("Поле 'oscarsCount' не может быть null.");
        }

        if (movie.getBudget() == null) {
            throw new ValidationException("Поле 'budget' не может быть null.");
        }

        if (movie.getMpaaRating() == null) {
            throw new ValidationException("Поле 'mpaaRating' не может быть null.");
        }

        if (movie.getOperator() == null) {
            throw new ValidationException("Поле 'operator' не может быть null. Укажите оператора.");
        }

        if (movie.getGenre() == null) {
            throw new ValidationException("Поле 'genre' не может быть null.");
        }

        if (movie.getOscarsCount() <= 0) {
            throw new ValidationException("Поле 'oscarsCount' должно быть больше 0.");
        }

        if (Float.compare(movie.getBudget(), 0.0f) <= 0) {
            throw new ValidationException("Поле 'budget' должно быть больше 0.");
        }

        if (movie.getTotalBoxOffice() != null && movie.getTotalBoxOffice() <= 0) {
            throw new ValidationException("Поле 'totalBoxOffice' должно быть null или больше 0.");
        }

        if (movie.getLength() != null && movie.getLength() <= 0) {
            throw new ValidationException("Поле 'length' должно быть null или больше 0.");
        }

        if (movie.getGoldenPalmCount() <= 0) {
            throw new ValidationException("Поле 'goldenPalmCount' должно быть больше 0.");
        }
    }
}
