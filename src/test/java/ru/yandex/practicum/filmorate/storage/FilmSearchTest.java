package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@AutoConfigureTestDatabase
@Import(FilmDbStorage.class)
class FilmSearchTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "MERGE INTO mpa (mpa_id, name) KEY (mpa_id) VALUES (3, 'PG-13')"
        );

        jdbcTemplate.update(
                "INSERT INTO directors (director_id, name) VALUES (?, ?)",
                101, "Кристофер Нолан"
        );

        addFilm(101, "Интерстеллар");
        addFilm(102, "Начало");
        addFilm(103, "Крадущийся тигр");
        addFilm(104, "Другой фильм");

        jdbcTemplate.update(
                "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)",
                101, 101
        );
        jdbcTemplate.update(
                "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)",
                102, 101
        );

        jdbcTemplate.update(
                "INSERT INTO users (user_id, email, login, birthday) "
                        + "VALUES (101, 'test@example.com', 'testuser', '2000-01-01')"
        );

        jdbcTemplate.update(
                "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)",
                102, 101
        );
    }

    private void addFilm(long id, String name) {
        jdbcTemplate.update(
                "INSERT INTO films "
                        + "(film_id, name, description, release_date, duration, mpa_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, name, "Описание", "2014-11-06", 120, 3
        );
    }

    @Test
    void shouldSearchByTitleSubstring() {
        List<Film> result = filmStorage.searchFilms(
                "крад", List.of("title")
        );

        assertEquals(1, result.size());
        assertEquals("Крадущийся тигр", result.get(0).getName());
    }

    @Test
    void shouldSearchByDirectorSubstring() {
        List<Film> result = filmStorage.searchFilms(
                "Нолан", List.of("director")
        );

        assertEquals(2, result.size());
    }

    @Test
    void shouldSearchByTitleIgnoringCase() {
        List<Film> result = filmStorage.searchFilms(
                "КРАД", List.of("title")
        );

        assertEquals(1, result.size());
        assertEquals("Крадущийся тигр", result.get(0).getName());
    }

    @Test
    void shouldSearchByDirectorAndTitle() {
        List<Film> result = filmStorage.searchFilms(
                "Начало", List.of("director", "title")
        );

        assertEquals(1, result.size());
        assertEquals("Начало", result.get(0).getName());
    }

    @Test
    void shouldSortSearchResultsByLikes() {
        List<Film> result = filmStorage.searchFilms(
                "Нолан", List.of("director")
        );

        assertEquals(2, result.size());
        assertEquals("Начало", result.get(0).getName());
        assertEquals("Интерстеллар", result.get(1).getName());
    }

    @Test
    void shouldReturnEmptyListWhenNothingFound() {
        List<Film> result = filmStorage.searchFilms(
                "Несуществующий фильм", List.of("title")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotDuplicateFilmWhenBothFieldsMatch() {
        jdbcTemplate.update(
                "UPDATE films SET name = ? WHERE film_id = ?",
                "Кристофер Нолан: фильм", 101
        );

        List<Film> result = filmStorage.searchFilms(
                "Нолан", List.of("director", "title")
        );

        assertEquals(2, result.size());
    }

    @Test
    void shouldSearchPercentAsLiteralCharacter() {
        addFilm(105, "Фильм 100% успех");

        List<Film> result = filmStorage.searchFilms(
                "100%", List.of("title")
        );

        assertEquals(1, result.size());
        assertEquals("Фильм 100% успех", result.get(0).getName());
    }

    @Test
    void shouldSearchUnderscoreAsLiteralCharacter() {
        addFilm(106, "Фильм test_version");

        List<Film> result = filmStorage.searchFilms(
                "test_", List.of("title")
        );

        assertEquals(1, result.size());
        assertEquals("Фильм test_version", result.get(0).getName());
    }
}