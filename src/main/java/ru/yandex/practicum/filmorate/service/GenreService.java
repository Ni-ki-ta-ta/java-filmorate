package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    @Qualifier("genreDbStorage")
    private final GenreStorage genreStorage;

    public List<Genre> findAll() {
        return genreStorage.findAll();
    }

    public Genre findById(Integer id) {
        return genreStorage.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Жанр с id " + id + " не найден"));
    }

    public List<Genre> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Integer> uniqueIds = ids.stream()
                .distinct()
                .toList();

        List<Genre> genres = genreStorage.findByIds(uniqueIds);

        if (genres.size() < uniqueIds.size()) {
            throw new NotFoundException("Один или несколько жанров не найдены");
        }

        return genres;
    }
}
