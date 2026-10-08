package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    Film create(Film film);

    Film update(Film film);

    Optional<Film> findById(Long id);

    List<Film> findAll();

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> findPopular(int count);

    List<Long> findLikedFilmIds(Long userId);
}
