package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private static final String FILM_ID_COLUMN = "film_id";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film create(Film film) {
        validateMpa(film);
        validateGenres(film);

        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setObject(3, film.getReleaseDate());
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());

            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("Не удалось получить id фильма после сохранения");
        }

        film.setId(keyHolder.getKey().longValue());

        saveGenres(film);

        return film;
    }

    @Override
    public Film update(Film film) {
        validateMpa(film);
        validateGenres(film);

        String sql = """
                UPDATE films
                SET name = ?,
                    description = ?,
                    release_date = ?,
                    duration = ?,
                    mpa_id = ?
                WHERE film_id = ?
                """;

        jdbcTemplate.update(
                sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        jdbcTemplate.update(
                "DELETE FROM film_genres WHERE film_id = ?",
                film.getId()
        );

        saveGenres(film);

        return film;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = """
                SELECT f.film_id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name
                FROM films f
                JOIN mpa m ON f.mpa_id = m.mpa_id
                WHERE f.film_id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Film film = new Film();

                    film.setId(rs.getLong(FILM_ID_COLUMN));
                    film.setName(rs.getString("name"));
                    film.setDescription(rs.getString("description"));
                    film.setReleaseDate(
                            rs.getDate("release_date").toLocalDate()
                    );
                    film.setDuration(rs.getInt("duration"));

                    Mpa mpa = new Mpa();
                    mpa.setId(rs.getInt("mpa_id"));
                    mpa.setName(rs.getString("mpa_name"));
                    film.setMpa(mpa);

                    film.setGenres(findGenresByFilmId(film.getId()));

                    return film;
                },
                id
        ).stream().findFirst();
    }

    @Override
    public List<Film> findAll() {
        String sql = """
                SELECT f.film_id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name
                FROM films f
                JOIN mpa m ON f.mpa_id = m.mpa_id
                ORDER BY f.film_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film film = new Film();

            film.setId(rs.getLong(FILM_ID_COLUMN));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(
                    rs.getDate("release_date").toLocalDate()
            );
            film.setDuration(rs.getInt("duration"));

            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("mpa_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);

            film.setGenres(findGenresByFilmId(film.getId()));

            return film;
        });
    }

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new NotFoundException("Рейтинг МРА не найден");
        }

        Integer mpaId = film.getMpa().getId();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mpa WHERE mpa_id = ?",
                Integer.class,
                mpaId
        );

        if (count == null || count == 0) {
            throw new NotFoundException("Рейтинг МРА не найден");
        }
    }

    private void validateGenres(Film film) {
        if (film.getGenres() == null) {
            return;
        }

        for (Genre genre : film.getGenres()) {
            if (genre == null || genre.getId() == null) {
                throw new NotFoundException("Жанр не найден");
            }

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM genres WHERE genre_id = ?",
                    Integer.class,
                    genre.getId()
            );

            if (count == null || count == 0) {
                throw new NotFoundException("Жанр не найден");
            }
        }
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO film_genres (film_id, genre_id)
                VALUES (?, ?)
                """;

        film.getGenres().stream()
                .filter(genre -> genre != null && genre.getId() != null)
                .map(Genre::getId)
                .distinct()
                .forEach(genreId -> jdbcTemplate.update(
                        sql,
                        film.getId(),
                        genreId
                ));
    }

    private List<Genre> findGenresByFilmId(Long filmId) {
        String sql = """
                SELECT g.genre_id,
                       g.name
                FROM genres g
                JOIN film_genres fg ON g.genre_id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.genre_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getInt("genre_id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = """
                INSERT INTO film_likes (film_id, user_id)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM film_likes
                    WHERE film_id = ? AND user_id = ?
                )
                """;

        jdbcTemplate.update(
                sql,
                filmId,
                userId,
                filmId,
                userId
        );
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = """
                DELETE FROM film_likes
                WHERE film_id = ? AND user_id = ?
                """;

        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> findPopular(int count) {
        String sql = """
                SELECT f.film_id,
                       COUNT(fl.user_id) AS likes_count
                FROM films f
                LEFT JOIN film_likes fl ON f.film_id = fl.film_id
                GROUP BY f.film_id
                ORDER BY likes_count DESC, f.film_id
                LIMIT ?
                """;

        List<Long> filmIds = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong(FILM_ID_COLUMN),
                count
        );

        return filmIds.stream()
                .map(this::findById)
                .flatMap(Optional::stream)
                .toList();
    }
}
