package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.enums.DirectorFilmsSortBy;
import ru.yandex.practicum.filmorate.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(FilmDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

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

        jdbcTemplate.update("""
                MERGE INTO directors (director_id, name)
                KEY (director_id)
                VALUES
                    (101, 'Christopher Nolan'),
                    (102, 'Guy Ritchie')
                """);
    }

    @Test
    void shouldCreateFilm() {
        Film film = createFilm("Тестовый фильм");

        Film savedFilm = filmDbStorage.create(film);

        assertTrue(
                savedFilm.getId() != null,
                "После создания фильму должен быть присвоен id"
        );
        assertEquals(
                "Тестовый фильм",
                savedFilm.getName(),
                "Название созданного фильма должно совпадать"
        );
        assertEquals(
                120,
                savedFilm.getDuration(),
                "Продолжительность фильма должна совпадать"
        );
    }

    @Test
    void shouldUpdateFilm() {
        Film film = filmDbStorage.create(createFilm("Старое название"));

        film.setName("Новое название");
        film.setDescription("Новое описание");
        film.setDuration(150);

        Film updatedFilm = filmDbStorage.update(film);

        Optional<Film> foundFilm = filmDbStorage.findById(updatedFilm.getId());

        assertTrue(
                foundFilm.isPresent(),
                "Обновлённый фильм должен находиться в базе"
        );
        assertEquals(
                "Новое название",
                foundFilm.get().getName(),
                "Название фильма должно обновиться"
        );
        assertEquals(
                "Новое описание",
                foundFilm.get().getDescription(),
                "Описание фильма должно обновиться"
        );
        assertEquals(
                150,
                foundFilm.get().getDuration(),
                "Продолжительность фильма должна обновиться"
        );
    }

    @Test
    void shouldFindFilmById() {


        Film savedFilm = filmDbStorage.create(createFilm("Тестовый фильм"));

        Optional<Film> foundFilm = filmDbStorage.findById(savedFilm.getId());

        assertTrue(
                foundFilm.isPresent(),
                "Созданный фильм можно найти по id"
        );
        assertEquals(
                savedFilm.getId(),
                foundFilm.get().getId(),
                "Id найденного фильма должен совпадать"
        );
        assertEquals(
                "Тестовый фильм",
                foundFilm.get().getName(),
                "Название найденного фильма должно совпадать"
        );
        assertEquals(
                "R",
                foundFilm.get().getMpa().getName(),
                "MPA фильма должен загружаться из базы"
        );
        assertEquals(
                2,
                foundFilm.get().getGenres().size(),
                "Жанры фильма должны загружаться из базы"
        );
        assertEquals(
                2,
                foundFilm.get().getDirector().size(),
                "Режисёры фильма должны загружаться из базы"
        );
    }

    @Test
    void shouldReturnEmptyWhenFilmDoesNotExist() {
        Optional<Film> foundFilm = filmDbStorage.findById(999999L);

        assertFalse(
                foundFilm.isPresent(),
                "Для несуществующего фильма должен возвращаться Optional.empty()"
        );
    }

    @Test
    void shouldFindAllFilms() {
        filmDbStorage.create(createFilm("Первый фильм"));
        filmDbStorage.create(createFilm("Второй фильм"));

        List<Film> films = filmDbStorage.findAll();

        assertEquals(
                2,
                films.size(),
                "В базе должно быть два созданных фильма"
        );
    }

    @Test
    void shouldAddLike() {
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        User user = createUser();

        filmDbStorage.addLike(film.getId(), user.getId());

        List<Film> popularFilms = filmDbStorage.findPopular(10);

        assertEquals(
                1,
                popularFilms.size(),
                "После добавления лайка фильм должен попасть в список популярных"
        );
        assertEquals(
                film.getId(),
                popularFilms.get(0).getId(),
                "Первым популярным фильмом должен быть фильм с лайком"
        );
    }

    @Test
    void shouldRemoveLike() {
        Film film = filmDbStorage.create(createFilm("Тестовый фильм"));
        User user = createUser();

        filmDbStorage.addLike(film.getId(), user.getId());
        filmDbStorage.removeLike(film.getId(), user.getId());

        Integer likesCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM film_likes
                        WHERE film_id = ? AND user_id = ?
                        """,
                Integer.class,
                film.getId(),
                user.getId()
        );

        assertEquals(
                0,
                likesCount,
                "После удаления лайка запись о лайке должна быть удалена из базы"
        );
    }

    @Test
    void shouldFindPopularFilms() {
        Film firstFilm = filmDbStorage.create(createFilm("Первый фильм"));
        Film secondFilm = filmDbStorage.create(createFilm("Второй фильм"));

        User firstUser = createUser();
        User secondUser = createUser();

        filmDbStorage.addLike(firstFilm.getId(), firstUser.getId());
        filmDbStorage.addLike(secondFilm.getId(), firstUser.getId());
        filmDbStorage.addLike(secondFilm.getId(), secondUser.getId());

        List<Film> popularFilms = filmDbStorage.findPopular(2);

        assertEquals(
                2,
                popularFilms.size(),
                "Должны вернуться два самых популярных фильма"
        );
        assertEquals(
                secondFilm.getId(),
                popularFilms.get(0).getId(),
                "Первым должен быть фильм с большим количеством лайков"
        );
        assertEquals(
                firstFilm.getId(),
                popularFilms.get(1).getId(),
                "Вторым должен быть фильм с меньшим количеством лайков"
        );
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

        Director director1 = new Director();
        director1.setId(101);

        Director director2 = new Director();
        director2.setId(102);

        film.setDirector(List.of(director1, director2));

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

    @Test
    void shouldFindDirectorFilmsSortedByYear() {
        Film newer = createFilm("Новый фильм");
        newer.setReleaseDate(LocalDate.of(2020, 1, 1));
        filmDbStorage.create(newer);

        Film older = createFilm("Старый фильм");
        older.setReleaseDate(LocalDate.of(1990, 1, 1));
        filmDbStorage.create(older);

        Film middle = createFilm("Средний фильм");
        middle.setReleaseDate(LocalDate.of(2005, 1, 1));
        filmDbStorage.create(middle);

        // Этот фильм не должен попасть в выборку.
        Film unrelated = createFilm("Без режиссёра");
        unrelated.setDirector(List.of());
        filmDbStorage.create(unrelated);

        List<Film> films = filmDbStorage.findByDirector(
                101,
                DirectorFilmsSortBy.YEAR
        );

        assertThat(films)
                .extracting(Film::getId)
                .containsExactly(
                        older.getId(),
                        middle.getId(),
                        newer.getId()
                );
    }

    @Test
    void shouldFindDirectorFilmsSortedByLikes() {
        Film withoutLikes = filmDbStorage.create(
                createFilm("Без лайков")
        );
        Film mostLiked = filmDbStorage.create(
                createFilm("Два лайка")
        );
        Film lessLiked = filmDbStorage.create(
                createFilm("Один лайк")
        );

        User firstUser = createUser();
        User secondUser = createUser();

        filmDbStorage.addLike(mostLiked.getId(), firstUser.getId());
        filmDbStorage.addLike(mostLiked.getId(), secondUser.getId());
        filmDbStorage.addLike(lessLiked.getId(), firstUser.getId());

        Film unrelated = createFilm("Без режиссёра");
        unrelated.setDirector(List.of());
        filmDbStorage.create(unrelated);

        filmDbStorage.addLike(unrelated.getId(), firstUser.getId());
        filmDbStorage.addLike(unrelated.getId(), secondUser.getId());

        List<Film> films = filmDbStorage.findByDirector(
                101,
                DirectorFilmsSortBy.LIKES
        );

        assertThat(films)
                .extracting(Film::getId)
                .containsExactly(
                        mostLiked.getId(),
                        lessLiked.getId(),
                        withoutLikes.getId()
                );
    }
}
