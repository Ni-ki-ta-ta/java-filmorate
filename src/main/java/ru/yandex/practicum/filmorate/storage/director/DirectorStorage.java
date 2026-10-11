package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;

public interface DirectorStorage {

    List<Director> findAll();

    Optional<Director> findById(Integer id);

    Director create(Director director);

    Director update(Director director);

    void delete(Integer id);
}
