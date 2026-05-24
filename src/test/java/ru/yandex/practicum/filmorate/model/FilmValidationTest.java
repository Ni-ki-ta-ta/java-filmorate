package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenNameIsBlank() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Описание");
        film.setDuration(100);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять фильм с пустым названием"
        );
    }

    @Test
    void shouldFailWhenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("a".repeat(201));
        film.setDuration(100);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять описание длиннее 200 символов"
        );
    }

    @Test
    void shouldPassWithValidFilm() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Хороший фильм");
        film.setDuration(120);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(
                violations.isEmpty(),
                "Валидация должна пропускать корректный фильм"
        );
    }

    @Test
    void shouldFailWhenReleaseDateIsNull() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setDuration(100);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять фильм без даты релиза"
        );
    }
}