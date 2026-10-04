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
                    (1, 'G'),
                    (2, 'PG'),
                    (3, 'PG-13'),
                    (4, 'R'),
                    (5, 'NC-17')
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
                mpaList.get(0).getId()
        );
        assertEquals(
                "G",
                mpaList.get(0).getName()
        );
    }

    @Test
    void shouldFindMpaById() {
        Optional<Mpa> mpa =
                mpaDbStorage.findById(3);

        assertTrue(mpa.isPresent());

        assertEquals(
                3,
                mpa.get().getId()
        );

        assertEquals(
                "PG-13",
                mpa.get().getName()
        );
    }

    @Test
    void shouldReturnEmptyWhenMpaDoesNotExist() {
        Optional<Mpa> mpa =
                mpaDbStorage.findById(999);

        assertFalse(mpa.isPresent());
    }

    @Test
    void shouldReturnMpaInIdOrder() {
        List<Mpa> mpaList =
                mpaDbStorage.findAll();

        assertEquals(
                1,
                mpaList.get(0).getId()
        );

        assertEquals(
                5,
                mpaList.get(4).getId()
        );

        assertEquals(
                "NC-17",
                mpaList.get(4).getName()
        );
    }
}
