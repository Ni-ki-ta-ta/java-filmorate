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

class UserValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenEmailIsBlank() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пустой email"
        );
    }

    @Test
    void shouldFailWhenEmailIsInvalid() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять email без символа @"
        );
    }

    @Test
    void shouldFailWhenLoginIsBlank() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLogin("");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пустой логин"
        );
    }

    @Test
    void shouldPassWithValidUser() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertTrue(
                violations.isEmpty(),
                "Валидация должна пропускать корректного пользователя"
        );
    }

    @Test
    void shouldFailWhenBirthdayIsNull() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLogin("login");

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(
                violations.isEmpty(),
                "Валидация должна отклонять пользователя без даты рождения"
        );
    }
}