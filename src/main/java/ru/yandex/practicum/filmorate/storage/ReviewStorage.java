package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {

    Review create(Review review);

    Review update(Review review);

    void delete(Long id);

    Optional<Review> findById(Long id);

    List<Review> findByFilmId(Long filmId, int count);

    void addReaction(Long reviewId, Long userId, boolean useful);

    void removeReaction(Long reviewId, Long userId, boolean useful);
}