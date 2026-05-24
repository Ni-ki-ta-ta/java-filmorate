package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {

    private static final LocalDate CINEMA_BIRTHDAY =
            LocalDate.of(1895, 12, 28);

    private final FilmService filmService;

    @GetMapping
    public List<Film> findAll() {
        log.info("Получен список фильмов");

        return filmService.findAll();
    }

    @GetMapping("/{id}")
    public Film findById(@PathVariable Long id) {
        log.info("Получен фильм с id {}", id);

        return filmService.findById(id);
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        validateFilm(film);

        log.info("Добавлен фильм: {}", film);

        return filmService.create(film);
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        validateFilm(film);

        log.info("Обновлен фильм: {}", film);

        return filmService.update(film);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        log.info(
                "Пользователь {} поставил лайк фильму {}",
                userId,
                id
        );

        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        log.info(
                "Пользователь {} удалил лайк у фильма {}",
                userId,
                id
        );

        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(
            @RequestParam(defaultValue = "10") int count
    ) {
        log.info("Запрошен список популярных фильмов");

        return filmService.getPopularFilms(count);
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.error("Дата релиза раньше 28 декабря 1895 года");

            throw new ValidationException(
                    "Дата релиза не может быть раньше 28 декабря 1895 года"
            );
        }

        if (film.getReleaseDate().isAfter(LocalDate.now().plusYears(1))) {
            log.error("Дата релиза фильма слишком далеко в будущем");

            throw new ValidationException(
                    "Дата релиза фильма не может быть слишком далеко в будущем"
            );
        }
    }
}