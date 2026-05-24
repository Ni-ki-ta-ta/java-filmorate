package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class User {

    private Long id;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный email")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
            message = "Email имеет неверный формат"
    )
    private String email;

    @NotBlank(message = "Логин не может быть пустым")
    private String login;

    private String name;

    @NotNull(message = "Дата рождения не может быть пустой")
    @Past(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;
}