package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorDbStorage directorDbStorage;

    public List<Director> findAll() {
        return directorDbStorage.findAll();
    }

    public Director findById(Integer id) {
        return directorDbStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Режисёра с id: " + id + " не найдено"));
    }

    public Director create(Director director) {
        return directorDbStorage.create(director);
    }

    public Director update(Director director) {
        validateId(director.getId());
        return directorDbStorage.update(director);
    }

    public void delete(Integer id) {
        validateId(id);
        directorDbStorage.delete(id);
    }

    private void validateId(Integer id) {
        if ((id == null) || (directorDbStorage.findById(id).isEmpty())) {
            throw new NotFoundException("Режисёр не найден");
        }
    }
}
