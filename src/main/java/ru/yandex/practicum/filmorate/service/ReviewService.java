package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Review create(Review review) {
        checkUserExists(review.getUserId());
        checkFilmExists(review.getFilmId());

        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        if (review.getReviewId() == null) {
            throw new NotFoundException("Не указан id отзыва");
        }

        findById(review.getReviewId());

        return reviewStorage.update(review);
    }

    public void delete(Long id) {
        findById(id);
        reviewStorage.delete(id);
    }

    public Review findById(Long id) {
        return reviewStorage.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Отзыв с id " + id + " не найден"));
    }

    public List<Review> findByFilmId(Long filmId, int count) {
        if (count <= 0) {
            throw new ValidationException(
                    "Количество отзывов должно быть больше 0"
            );
        }

        if (filmId != null) {
            checkFilmExists(filmId);
        }

        return reviewStorage.findByFilmId(filmId, count);
    }

    public void addLike(Long id, Long userId) {
        addReaction(id, userId, true);
    }

    public void addDislike(Long id, Long userId) {
        addReaction(id, userId, false);
    }

    public void removeLike(Long id, Long userId) {
        removeReaction(id, userId, true);
    }

    public void removeDislike(Long id, Long userId) {
        removeReaction(id, userId, false);
    }

    private void addReaction(Long id, Long userId, boolean useful) {
        findById(id);
        checkUserExists(userId);

        reviewStorage.addReaction(id, userId, useful);
    }

    private void removeReaction(Long id, Long userId, boolean useful) {
        findById(id);
        checkUserExists(userId);

        reviewStorage.removeReaction(id, userId, useful);
    }

    private void checkUserExists(Long userId) {
        userStorage.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private void checkFilmExists(Long filmId) {
        filmStorage.findById(filmId)
                .orElseThrow(() ->
                        new NotFoundException("Фильм с id " + filmId + " не найден"));
    }
}