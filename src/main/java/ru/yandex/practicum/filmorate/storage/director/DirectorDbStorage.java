package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Director> mapper;

    @Override
    public List<Director> findAll() {
        String sql = "SELECT * FROM directors";
        return jdbcTemplate.query(sql, mapper);
    }

    @Override
    public Optional<Director> findById(Integer id) {
        String sql = """
                SELECT * FROM directors
                WHERE director_id = ?
                """;
        try {
            Director director = jdbcTemplate.queryForObject(sql, mapper, id);
            return Optional.ofNullable(director);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public Director create(Director director) {
        String sql = """
                INSERT INTO directors (name)
                VALUES (?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("Не удалось получить id режисёра");
        }

        director.setId(keyHolder.getKey().intValue());
        return director;
    }

    @Override
    public Director update(Director director) {
        String sql = """
                UPDATE directors
                SET name = ?
                WHERE director_id = ?
                """;
        int rowsUpdated = jdbcTemplate.update(sql, director.getName(), director.getId());
        if (rowsUpdated == 0) {
            throw new RuntimeException("Не удалось обновить данные");
        }
        return director;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM directors WHERE director_id = ?";
        jdbcTemplate.update(sql, id);
    }


}
