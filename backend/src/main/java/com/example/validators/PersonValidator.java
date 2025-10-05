package com.example.validators;

import com.example.models.Location;
import com.example.models.Person;
import com.example.validators.exceptions.ValidationException;

public class PersonValidator {

    public static void validate(Person person) {
        if (person == null) {
            throw new ValidationException("Объект Person не может быть null.");
        }

        if (person.getName() == null || person.getName().trim().isEmpty()) {
            throw new ValidationException("Поле 'name' не может быть пустым.");
        }

        if (person.getHairColor() == null) {
            throw new ValidationException("Поле 'hairColor' не может быть null.");
        }

        Location location = person.getLocation();
        if (location == null) {
            throw new ValidationException("Поле 'location' не может быть null.");
        }

        if (Double.isNaN(location.getX())
                || Double.isInfinite(location.getX())
                || Double.isNaN(location.getY())
                || Double.isInfinite(location.getY())
                || Double.isNaN(location.getZ())
                || Double.isInfinite(location.getZ())) {
            throw new ValidationException(
                    "Координаты 'location' (x, y, z) должны быть корректными числами.");
        }
    }
}
