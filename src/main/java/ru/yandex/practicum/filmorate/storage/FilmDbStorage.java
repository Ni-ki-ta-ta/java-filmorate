package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.enums.DirectorFilmsSortBy;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private static final String FILM_ID_COLUMN = "film_id";

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public Film create(Film film) {
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
            throw new IllegalStateException("Не удалось получить id фильма");
        }

        film.setId(keyHolder.getKey().longValue());

        saveGenres(film);
        saveDirectors(film);

        fillDirectors(List.of(film));
        return film;
    }

    @Transactional
    @Override
    public Film update(Film film) {
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
        jdbcTemplate.update(
                "DELETE FROM film_directors WHERE film_id = ?",
                film.getId()
        );

        saveGenres(film);
        saveDirectors(film);

        fillDirectors(List.of(film));
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

        List<Film> films = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapFilm(rs),
                id
        );

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, List<Genre>> genresByFilmId =
                findGenresByFilmIds(List.of(id));

        Map<Long, List<Director>> directorsByFilmId =
                findDirectorsByFilmIds(List.of(id));

        films.get(0).setGenres(
                genresByFilmId.getOrDefault(id, List.of())
        );

        films.get(0).setDirectors(
                directorsByFilmId.getOrDefault(id, List.of())
        );

        return Optional.of(films.get(0));
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

        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> mapFilm(rs));

        fillGenres(films);
        fillDirectors(films);

        return films;
    }

    private Film mapFilm(java.sql.ResultSet rs) throws java.sql.SQLException {
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

        return film;
    }

    private void fillGenres(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        Map<Long, List<Genre>> genresByFilmId =
                findGenresByFilmIds(filmIds);

        for (Film film : films) {
            film.setGenres(
                    genresByFilmId.getOrDefault(film.getId(), List.of())
            );
        }
    }

    private void fillDirectors(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        Map<Long, List<Director>> directorsByFilmId =
                findDirectorsByFilmIds(filmIds);

        for (Film film : films) {
            film.setDirectors(
                    directorsByFilmId.getOrDefault(film.getId(), List.of())
            );
        }
    }

    private Map<Long, List<Genre>> findGenresByFilmIds(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT fg.film_id,
                       g.genre_id,
                       g.name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.genre_id
                """.formatted(placeholders);

        Map<Long, List<Genre>> result = new HashMap<>();

        jdbcTemplate.query(
                sql,
                rs -> {
                    Long filmId = rs.getLong(FILM_ID_COLUMN);

                    Genre genre = new Genre();
                    genre.setId(rs.getInt("genre_id"));
                    genre.setName(rs.getString("name"));

                    result.computeIfAbsent(
                            filmId,
                            key -> new java.util.ArrayList<>()
                    ).add(genre);
                },
                filmIds.toArray()
        );

        return result;
    }

    private Map<Long, List<Director>> findDirectorsByFilmIds(List<Long> filmIds) {
        if (filmIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = filmIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT fd.film_id,
                       d.director_id,
                       d.name
                FROM film_directors fd
                JOIN directors d ON fd.director_id = d.director_id
                WHERE fd.film_id IN (%s)
                ORDER BY fd.film_id, d.director_id
                """.formatted(placeholders);

        Map<Long, List<Director>> result = new HashMap<>();

        jdbcTemplate.query(
                sql,
                rs -> {
                    Long filmId = rs.getLong(FILM_ID_COLUMN);

                    Director director = new Director();
                    director.setId(rs.getInt("director_id"));
                    director.setName(rs.getString("name"));

                    result.computeIfAbsent(
                            filmId,
                            key -> new java.util.ArrayList<>()
                    ).add(director);
                },
                filmIds.toArray()
        );

        return result;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO film_genres (film_id, genre_id)
                VALUES (?, ?)
                """;

        List<Object[]> batchArgs = film.getGenres().stream()
                .filter(genre -> genre != null && genre.getId() != null)
                .map(Genre::getId)
                .distinct()
                .map(genreId -> new Object[]{
                        film.getId(),
                        genreId
                })
                .toList();

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO film_directors (film_id, director_id)
                VALUES (?, ?)
                """;

        List<Object[]> batchArgs = film.getDirectors().stream()
                .filter(director -> (director != null) && (director.getId() != null))
                .map(Director::getId)
                .distinct()
                .map(directorId -> new Object[]{
                        film.getId(),
                        directorId
                })
                .toList();
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }

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
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name,
                       COUNT(fl.user_id) AS likes_count
                FROM films f
                JOIN mpa m ON f.mpa_id = m.mpa_id
                LEFT JOIN film_likes fl ON f.film_id = fl.film_id
                GROUP BY f.film_id,
                         f.name,
                         f.description,
                         f.release_date,
                         f.duration,
                         m.mpa_id,
                         m.name
                ORDER BY likes_count DESC, f.film_id
                LIMIT ?
                """;

        List<Film> films = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapFilm(rs),
                count
        );

        fillGenres(films);
        fillDirectors(films);

        return films;
    }

    @Override
    public List<Film> findByDirector(Integer directorId, DirectorFilmsSortBy sortBy) {

        String orderBy = switch (sortBy) {
            case YEAR -> "EXTRACT(YEAR FROM f.release_date) ASC, f.film_id ASC";
            case LIKES -> "likes_count DESC, f.film_id ASC";
        };

        String sql = """
                SELECT f.film_id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       m.mpa_id,
                       m.name AS mpa_name,
                       COALESCE(l.likes_count, 0) AS likes_count
                FROM films f
                JOIN mpa m ON m.mpa_id = f.mpa_id
                JOIN film_directors fd ON fd.film_id = f.film_id
                LEFT JOIN (
                    SELECT film_id, COUNT(*) AS likes_count
                    FROM film_likes
                    GROUP BY film_id
                ) l ON l.film_id = f.film_id
                WHERE fd.director_id = ?
                ORDER BY %s
                """.formatted(orderBy);

        List<Film> films = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapFilm(rs),
                directorId
        );

        fillGenres(films);
        fillDirectors(films);

        return films;
    }

    @Override
    public List<Film> searchFilms(String query, List<String> searchBy) {
        boolean searchTitle = searchBy.contains("title");
        boolean searchDirector = searchBy.contains("director");

        String searchCondition;

        if (searchTitle && searchDirector) {
            searchCondition = """
                LOWER(f.name) LIKE ? ESCAPE '\\' OR EXISTS (
                    SELECT 1
                    FROM film_directors fd
                    JOIN directors d ON d.director_id = fd.director_id
                    WHERE fd.film_id = f.film_id
                      AND LOWER(d.name) LIKE ? ESCAPE '\\'
                )
                """;
        } else if (searchTitle) {
            searchCondition = "LOWER(f.name) LIKE ? ESCAPE '\\'";
        } else {
            searchCondition = """
                EXISTS (
                    SELECT 1
                    FROM film_directors fd
                    JOIN directors d ON d.director_id = fd.director_id
                    WHERE fd.film_id = f.film_id
                      AND LOWER(d.name) LIKE ? ESCAPE '\\'
                )
                """;
        }

        String sql = """
            SELECT f.film_id,
                   f.name,
                   f.description,
                   f.release_date,
                   f.duration,
                   m.mpa_id,
                   m.name AS mpa_name,
                   COUNT(fl.user_id) AS likes_count
            FROM films f
            JOIN mpa m ON f.mpa_id = m.mpa_id
            LEFT JOIN film_likes fl ON f.film_id = fl.film_id
            WHERE %s
            GROUP BY f.film_id,
                     f.name,
                     f.description,
                     f.release_date,
                     f.duration,
                     m.mpa_id,
                     m.name
            ORDER BY likes_count DESC, f.film_id ASC
            """.formatted(searchCondition);

        String escapedQuery = query.toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        String searchQuery = "%" + escapedQuery + "%";

        List<Film> films;

        if (searchTitle && searchDirector) {
            films = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> mapFilm(rs),
                    searchQuery,
                    searchQuery
            );
        } else {
            films = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> mapFilm(rs),
                    searchQuery
            );
        }

        fillGenres(films);
        fillDirectors(films);

        return films;
    }
}
