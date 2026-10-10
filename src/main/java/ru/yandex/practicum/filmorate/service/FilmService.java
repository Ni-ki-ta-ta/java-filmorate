package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FilmService {

    private static final LocalDate CINEMA_BIRTHDAY =
            LocalDate.of(1895, 12, 28);

    private final EventStorage eventStorage;

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    private final GenreService genreService;
    private final MpaService mpaService;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);

        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);

        if (film.getId() == null || filmStorage.findById(film.getId()).isEmpty()) {
            throw new NotFoundException("Фильм не найден");
        }

        return filmStorage.update(film);
    }

    public Film findById(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Фильм с id " + id + " не найден"));
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException(
                    "Дата релиза не может быть раньше 28 декабря 1895 года"
            );
        }

        if (film.getReleaseDate().isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "Дата релиза фильма не может быть в будущем"
            );
        }
    }

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new NotFoundException("Рейтинг MPA не найден");
        }

        mpaService.findById(film.getMpa().getId());
    }

    private void validateGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        if (film.getGenres().stream()
                .anyMatch(genre -> genre == null || genre.getId() == null)) {
            throw new NotFoundException("Жанр не найден");
        }

        List<Integer> genreIds = film.getGenres().stream()
                .map(Genre::getId)
                .toList();

        genreService.findByIds(genreIds);
    }

    public void addLike(Long filmId, Long userId) {
        findById(filmId);

        userStorage.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id " + userId + " не найден"
                        ));

        if (!filmStorage.hasLike(filmId, userId)) {
            filmStorage.addLike(filmId, userId);
            eventStorage.addEvent(new Event(
                    null,
                    System.currentTimeMillis(),
                    userId,
                    EventType.LIKE,
                    Operation.ADD,
                    filmId
            ));
        }
    }

    public void removeLike(Long filmId, Long userId) {
        findById(filmId);

        userStorage.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Пользователь с id " + userId + " не найден"
                        ));

        if (filmStorage.hasLike(filmId, userId)) {
            filmStorage.removeLike(filmId, userId);
            eventStorage.addEvent(new Event(
                    null,
                    System.currentTimeMillis(),
                    userId,
                    EventType.LIKE,
                    Operation.REMOVE,
                    filmId
            ));
        }
    }

    public List<Film> findPopular(int count) {
        if (count <= 0) {
            throw new ValidationException(
                    "Количество популярных фильмов должно быть больше 0"
            );
        }

        return filmStorage.findPopular(count);
    }
}
