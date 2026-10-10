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
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Review create(@Valid @RequestBody Review review) {
        Review createdReview = reviewService.create(review);
        log.info("Добавлен отзыв: {}", createdReview);
        return createdReview;
    }

    @PutMapping
    public Review update(@Valid @RequestBody Review review) {
        Review updatedReview = reviewService.update(review);
        log.info("Обновлён отзыв: {}", updatedReview);
        return updatedReview;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.info("Удаление отзыва {}", id);
        reviewService.delete(id);
    }

    @GetMapping("/{id}")
    public Review findById(@PathVariable Long id) {
        log.info("Получен отзыв с id {}", id);
        return reviewService.findById(id);
    }

    @GetMapping
    public List<Review> findByFilmId(
            @RequestParam(required = false) Long filmId,
            @RequestParam(defaultValue = "10") int count
    ) {
        log.info("Запрошены отзывы: filmId={}, count={}", filmId, count);
        return reviewService.findByFilmId(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Пользователь {} поставил лайк отзыву {}", userId, id);
        reviewService.addLike(id, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Пользователь {} поставил дизлайк отзыву {}", userId, id);
        reviewService.addDislike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Пользователь {} убрал лайк с отзыва {}", userId, id);
        reviewService.removeLike(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void removeDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Пользователь {} убрал дизлайк с отзыва {}", userId, id);
        reviewService.removeDislike(id, userId);
    }
}