package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.enums.DirectorFilmsSortBy;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FilmService {

    private static final LocalDate CINEMA_BIRTHDAY =
            LocalDate.of(1895, 12, 28);

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    private final DirectorService directorService;
    private final DirectorDbStorage directorStorage;

    private final GenreService genreService;
    private final MpaService mpaService;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);
        validateDirectors(film);

        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);
        validateDirectors(film);

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

    private void validateDirectors(Film film) {
        if (film.getDirectors() == null) {
            film.setDirectors(new ArrayList<>());
            return;
        }

        for (Director director : film.getDirectors()) {
            if ((director == null) || (director.getId() == null)) {
                throw new ValidationException("id режисёра не указано");
            }
            directorService.findById(director.getId());
        }

    }

    public void addLike(Long filmId, Long userId) {
        findById(filmId);

        userStorage.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + userId + " не найден"));

        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        findById(filmId);

        userStorage.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + userId + " не найден"));

        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> findPopular(int count) {
        if (count <= 0) {
            throw new ValidationException(
                    "Количество популярных фильмов должно быть больше 0"
            );
        }

        return filmStorage.findPopular(count);
    }

    public List<Film> getDirectorFilms(Integer directorId, DirectorFilmsSortBy sortBy) {
        if ((directorId == null) || directorStorage.findById(directorId).isEmpty()) {
            throw new NotFoundException("Режисёр с id: " + directorId + " не найден");
        }
        return filmStorage.findByDirector(directorId, sortBy);
    }

    public List<Film> searchFilms(String query, String by) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Поисковый запрос не может быть пустым");
        }

        if (by == null || by.isBlank()) {
            throw new ValidationException("Не указан тип поиска");
        }

        List<String> searchBy = List.of(by.split(",", -1));

        if (searchBy.size() > 2
                || searchBy.stream().anyMatch(value ->
                !value.equals("title") && !value.equals("director"))
                || searchBy.stream().distinct().count() != searchBy.size()) {
            throw new ValidationException("Недопустимый тип поиска: " + by);
        }

        return filmStorage.searchFilms(query, searchBy);
    }
}
