package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        FilmDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserServiceRecommendationTest {

    private final UserDbStorage userDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final JdbcTemplate jdbcTemplate;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDbStorage, filmDbStorage);

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
    void shouldRecommendFilmsLikedBySimilarUser() {
        Film film1 = filmDbStorage.create(createFilm("Фильм 1"));
        Film film2 = filmDbStorage.create(createFilm("Фильм 2"));
        Film film3 = filmDbStorage.create(createFilm("Фильм 3"));
        Film film4 = filmDbStorage.create(createFilm("Фильм 4"));

        User user1 = userDbStorage.create(createUser("user1"));
        User user2 = userDbStorage.create(createUser("user2"));
        User user3 = userDbStorage.create(createUser("user3"));

        filmDbStorage.addLike(film1.getId(), user1.getId());
        filmDbStorage.addLike(film2.getId(), user1.getId());

        filmDbStorage.addLike(film1.getId(), user2.getId());
        filmDbStorage.addLike(film2.getId(), user2.getId());
        filmDbStorage.addLike(film3.getId(), user2.getId());
        filmDbStorage.addLike(film4.getId(), user2.getId());

        filmDbStorage.addLike(film1.getId(), user3.getId());

        List<Film> recommendations =
                userService.getRecommendations(user1.getId());

        assertEquals(
                2,
                recommendations.size(),
                "Количество рекомендаций должно быть равно двум"
        );

        assertTrue(
                recommendations.stream()
                        .anyMatch(film -> film.getId().equals(film3.getId())),
                "Третий фильм должен быть в рекомендациях"
        );

        assertTrue(
                recommendations.stream()
                        .anyMatch(film -> film.getId().equals(film4.getId())),
                "Четвёртый фильм должен быть в рекомендациях"
        );

        assertTrue(
                recommendations.stream()
                        .noneMatch(film -> film.getId().equals(film1.getId())),
                "Первый фильм не должен быть в рекомендациях"
        );

        assertTrue(
                recommendations.stream()
                        .noneMatch(film -> film.getId().equals(film2.getId())),
                "Второй фильм не должен быть в рекомендациях"
        );
    }

    @Test
    void shouldReturnEmptyRecommendationsWhenUserHasNoLikes() {
        User user = userDbStorage.create(createUser("user"));

        List<Film> recommendations =
                userService.getRecommendations(user.getId());

        assertTrue(
                recommendations.isEmpty(),
                "Для пользователя без лайков рекомендации должны быть пустыми"
        );
    }

    private User createUser(String login) {
        User user = new User();
        user.setEmail(login + "@test.com");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание фильма");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        mpa.setName("G");
        film.setMpa(mpa);

        return film;
    }
}
