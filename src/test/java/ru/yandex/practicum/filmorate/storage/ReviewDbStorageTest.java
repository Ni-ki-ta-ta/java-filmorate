package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase
@Import({ReviewDbStorage.class, FilmDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReviewDbStorageTest {

    private final ReviewDbStorage reviewDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                MERGE INTO mpa (mpa_id, name)
                KEY (mpa_id)
                VALUES
                    (1, 'G'),
                    (2, 'PG'),
                    (3, 'PG-13'),
                    (4, 'R'),
                    (5, 'NC-17')
                """);

        jdbcTemplate.update("""
                MERGE INTO genres (genre_id, name)
                KEY (genre_id)
                VALUES
                    (1, 'Комедия'),
                    (2, 'Драма'),
                    (3, 'Мультфильм'),
                    (4, 'Триллер'),
                    (5, 'Документальный'),
                    (6, 'Боевик')
                """);
    }

    @Test
    void shouldCreateReview() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));

        Review savedReview = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Отличный фильм", true)
        );

        assertNotNull(
                savedReview.getReviewId(),
                "После создания отзыву должен быть присвоен id"
        );
        assertEquals(
                "Отличный фильм",
                savedReview.getContent(),
                "Текст созданного отзыва должен совпадать"
        );
        assertTrue(
                savedReview.getIsPositive(),
                "Тип отзыва должен совпадать"
        );
        assertEquals(
                0,
                savedReview.getUseful(),
                "У нового отзыва рейтинг полезности должен быть 0"
        );
    }

    @Test
    void shouldUpdateReviewContentAndTypeOnly() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review review = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Старый текст", true)
        );

        review.setContent("Новый текст");
        review.setIsPositive(false);

        Review updatedReview = reviewDbStorage.update(review);

        assertEquals(
                "Новый текст",
                updatedReview.getContent(),
                "Текст отзыва должен обновиться"
        );
        assertFalse(
                updatedReview.getIsPositive(),
                "Тип отзыва должен обновиться"
        );
        assertEquals(
                user.getId(),
                updatedReview.getUserId(),
                "Автор отзыва не должен меняться"
        );
        assertEquals(
                film.getId(),
                updatedReview.getFilmId(),
                "Фильм отзыва не должен меняться"
        );
    }

    @Test
    void shouldFindReviewById() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review savedReview = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Текст", true)
        );

        Optional<Review> foundReview = reviewDbStorage.findById(savedReview.getReviewId());

        assertTrue(
                foundReview.isPresent(),
                "Созданный отзыв можно найти по id"
        );
        assertEquals(
                savedReview.getReviewId(),
                foundReview.get().getReviewId(),
                "Id найденного отзыва должен совпадать"
        );
    }

    @Test
    void shouldReturnEmptyWhenReviewDoesNotExist() {
        Optional<Review> foundReview = reviewDbStorage.findById(9999L);

        assertTrue(
                foundReview.isEmpty(),
                "Несуществующий отзыв не должен находиться"
        );
    }

    @Test
    void shouldDeleteReviewWithItsReactions() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review review = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Текст", true)
        );
        reviewDbStorage.addReaction(review.getReviewId(), user.getId(), true);

        reviewDbStorage.delete(review.getReviewId());

        assertTrue(
                reviewDbStorage.findById(review.getReviewId()).isEmpty(),
                "Удалённый отзыв не должен находиться"
        );

        Integer reactionsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_likes WHERE review_id = ?",
                Integer.class,
                review.getReviewId()
        );

        assertEquals(
                0,
                reactionsCount,
                "Реакции на удалённый отзыв должны удаляться вместе с ним"
        );
    }

    @Test
    void shouldCalculateUsefulFromLikesAndDislikes() {
        User author = createUser();
        User firstUser = createUser();
        User secondUser = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review review = reviewDbStorage.create(
                createReview(author.getId(), film.getId(), "Текст", true)
        );

        reviewDbStorage.addReaction(review.getReviewId(), author.getId(), true);
        reviewDbStorage.addReaction(review.getReviewId(), firstUser.getId(), true);
        reviewDbStorage.addReaction(review.getReviewId(), secondUser.getId(), false);

        Review foundReview = reviewDbStorage.findById(review.getReviewId()).orElseThrow();

        assertEquals(
                1,
                foundReview.getUseful(),
                "Рейтинг должен быть равен лайки минус дизлайки (2 - 1)"
        );
    }

    @Test
    void shouldReplaceLikeWithDislike() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review review = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Текст", true)
        );

        reviewDbStorage.addReaction(review.getReviewId(), user.getId(), true);
        reviewDbStorage.addReaction(review.getReviewId(), user.getId(), false);

        Review foundReview = reviewDbStorage.findById(review.getReviewId()).orElseThrow();

        assertEquals(
                -1,
                foundReview.getUseful(),
                "Дизлайк должен заменить лайк того же пользователя"
        );
    }

    @Test
    void shouldRemoveOnlyReactionOfRequestedType() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review review = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Текст", true)
        );
        reviewDbStorage.addReaction(review.getReviewId(), user.getId(), true);

        reviewDbStorage.removeReaction(review.getReviewId(), user.getId(), false);

        assertEquals(
                1,
                reviewDbStorage.findById(review.getReviewId()).orElseThrow().getUseful(),
                "Удаление дизлайка не должно убирать лайк"
        );

        reviewDbStorage.removeReaction(review.getReviewId(), user.getId(), true);

        assertEquals(
                0,
                reviewDbStorage.findById(review.getReviewId()).orElseThrow().getUseful(),
                "После удаления лайка рейтинг должен вернуться к 0"
        );
    }

    @Test
    void shouldSortReviewsByUsefulDescending() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        Review lessUseful = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Первый", true)
        );
        Review moreUseful = reviewDbStorage.create(
                createReview(user.getId(), film.getId(), "Второй", true)
        );

        reviewDbStorage.addReaction(moreUseful.getReviewId(), user.getId(), true);

        List<Review> reviews = reviewDbStorage.findByFilmId(film.getId(), 10);

        assertEquals(2, reviews.size(), "Должны вернуться оба отзыва");
        assertEquals(
                moreUseful.getReviewId(),
                reviews.get(0).getReviewId(),
                "Первым должен идти отзыв с большим рейтингом"
        );
        assertEquals(
                lessUseful.getReviewId(),
                reviews.get(1).getReviewId(),
                "Вторым должен идти отзыв с меньшим рейтингом"
        );
    }

    @Test
    void shouldFindOnlyReviewsOfRequestedFilm() {
        User user = createUser();
        Film firstFilm = filmDbStorage.create(createFilm("Первый фильм"));
        Film secondFilm = filmDbStorage.create(createFilm("Второй фильм"));
        reviewDbStorage.create(
                createReview(user.getId(), firstFilm.getId(), "Про первый", true)
        );
        reviewDbStorage.create(
                createReview(user.getId(), secondFilm.getId(), "Про второй", true)
        );

        List<Review> reviews = reviewDbStorage.findByFilmId(firstFilm.getId(), 10);

        assertEquals(
                1,
                reviews.size(),
                "Должны вернуться только отзывы на запрошенный фильм"
        );
        assertEquals(
                firstFilm.getId(),
                reviews.get(0).getFilmId(),
                "Отзыв должен относиться к запрошенному фильму"
        );
    }

    @Test
    void shouldFindReviewsOfAllFilmsWhenFilmIdIsNull() {
        User user = createUser();
        Film firstFilm = filmDbStorage.create(createFilm("Первый фильм"));
        Film secondFilm = filmDbStorage.create(createFilm("Второй фильм"));
        reviewDbStorage.create(
                createReview(user.getId(), firstFilm.getId(), "Про первый", true)
        );
        reviewDbStorage.create(
                createReview(user.getId(), secondFilm.getId(), "Про второй", true)
        );

        List<Review> reviews = reviewDbStorage.findByFilmId(null, 10);

        assertEquals(
                2,
                reviews.size(),
                "Без filmId должны вернуться отзывы всех фильмов"
        );
    }

    @Test
    void shouldLimitReviewsByCount() {
        User user = createUser();
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        reviewDbStorage.create(createReview(user.getId(), film.getId(), "Один", true));
        reviewDbStorage.create(createReview(user.getId(), film.getId(), "Два", true));
        reviewDbStorage.create(createReview(user.getId(), film.getId(), "Три", true));

        List<Review> reviews = reviewDbStorage.findByFilmId(film.getId(), 2);

        assertEquals(
                2,
                reviews.size(),
                "Количество отзывов не должно превышать count"
        );
    }

    private Review createReview(
            Long userId,
            Long filmId,
            String content,
            boolean isPositive
    ) {
        Review review = new Review();
        review.setContent(content);
        review.setIsPositive(isPositive);
        review.setUserId(userId);
        review.setFilmId(filmId);
        return review;
    }

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Тестовое описание");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(4);
        film.setMpa(mpa);

        Genre comedy = new Genre();
        comedy.setId(1);

        Genre drama = new Genre();
        drama.setId(2);

        film.setGenres(List.of(comedy, drama));

        return film;
    }

    private User createUser() {
        User user = new User();
        user.setEmail("user" + System.nanoTime() + "@test.com");
        user.setLogin("user" + System.nanoTime());
        user.setName("Тестовый пользователь");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        jdbcTemplate.update(
                """
                        INSERT INTO users (email, login, name, birthday)
                        VALUES (?, ?, ?, ?)
                        """,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday()
        );

        Long userId = jdbcTemplate.queryForObject(
                """
                        SELECT user_id
                        FROM users
                        WHERE email = ?
                        """,
                Long.class,
                user.getEmail()
        );

        user.setId(userId);

        return user;
    }
}