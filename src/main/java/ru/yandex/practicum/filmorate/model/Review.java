package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Review {

    private Long reviewId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Тип отзыва должен быть указан")
    @JsonProperty("isPositive")
    private Boolean isPositive;

    @NotNull(message = "Пользователь должен быть указан")
    private Long userId;

    @NotNull(message = "Фильм должен быть указан")
    private Long filmId;

    private int useful;
}