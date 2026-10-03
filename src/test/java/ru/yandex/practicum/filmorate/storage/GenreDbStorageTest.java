package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(GenreDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreDbStorageTest {

    private final GenreDbStorage genreDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
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
    void shouldFindAllGenres() {
        List<Genre> genres = genreDbStorage.findAll();

        assertEquals(
                6,
                genres.size(),
                "Должно быть шесть жанров"
        );
        assertEquals(
                1,
                genres.get(0).getId(),
                "Первый жанр должен иметь id 1"
        );
        assertEquals(
                "Комедия",
                genres.get(0).getName(),
                "Первый жанр должен называться «Комедия»"
        );
    }

    @Test
    void shouldFindGenreById() {
        Optional<Genre> genre = genreDbStorage.findById(1);

        assertTrue(
                genre.isPresent(),
                "Жанр с существующим id должен быть найден"
        );
        assertEquals(
                1,
                genre.get().getId(),
                "Id найденного жанра должен совпадать"
        );
        assertEquals(
                "Комедия",
                genre.get().getName(),
                "Название найденного жанра должно совпадать"
        );
    }

    @Test
    void shouldReturnEmptyWhenGenreDoesNotExist() {
        Optional<Genre> genre = genreDbStorage.findById(999);

        assertFalse(
                genre.isPresent(),
                "Для несуществующего жанра должен возвращаться Optional.empty()"
        );
    }

    @Test
    void shouldReturnGenresInIdOrder() {
        List<Genre> genres = genreDbStorage.findAll();

        assertEquals(
                1,
                genres.get(0).getId(),
                "Первый жанр должен иметь id 1"
        );
        assertEquals(
                6,
                genres.get(5).getId(),
                "Последний жанр должен иметь id 6"
        );
    }
}
