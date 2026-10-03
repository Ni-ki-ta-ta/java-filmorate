package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(MpaDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaDbStorageTest {

    private final MpaDbStorage mpaDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("""
                MERGE INTO mpa (mpa_id, name)
                KEY (mpa_id)
                VALUES
                    (1, '0+'),
                    (2, '6+'),
                    (3, '12+'),
                    (4, '16+'),
                    (5, '18+')
                """);
    }

    @Test
    void shouldFindAllMpa() {
        List<Mpa> mpaList = mpaDbStorage.findAll();

        assertEquals(
                5,
                mpaList.size(),
                "Должно быть пять рейтингов MPA"
        );
        assertEquals(
                1,
                mpaList.get(0).getId(),
                "Первый рейтинг должен иметь id 1"
        );
        assertEquals(
                "0+",
                mpaList.get(0).getName(),
                "Первый рейтинг должен называться «0+»"
        );
    }

    @Test
    void shouldFindMpaById() {
        Optional<Mpa> mpa = mpaDbStorage.findById(2);

        assertTrue(
                mpa.isPresent(),
                "Рейтинг с существующим id должен быть найден"
        );
        assertEquals(
                2,
                mpa.get().getId(),
                "Id найденного рейтинга должен совпадать"
        );
        assertEquals(
                "6+",
                mpa.get().getName(),
                "Название найденного рейтинга должно совпадать"
        );
    }

    @Test
    void shouldReturnEmptyWhenMpaDoesNotExist() {
        Optional<Mpa> mpa = mpaDbStorage.findById(999);

        assertFalse(
                mpa.isPresent(),
                "Для несуществующего рейтинга должен возвращаться Optional.empty()"
        );
    }

    @Test
    void shouldReturnMpaInIdOrder() {
        List<Mpa> mpaList = mpaDbStorage.findAll();

        assertEquals(
                1,
                mpaList.get(0).getId(),
                "Первый рейтинг должен иметь id 1"
        );
        assertEquals(
                5,
                mpaList.get(4).getId(),
                "Последний рейтинг должен иметь id 5"
        );
    }
}
