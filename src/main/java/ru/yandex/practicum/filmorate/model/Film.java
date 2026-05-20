package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Film {

    private Long id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(message = "Продолжительность не может быть пустой")
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;

    @NotNull(message = "Дата релиза не может быть пустой")
    private LocalDate releaseDate;
}