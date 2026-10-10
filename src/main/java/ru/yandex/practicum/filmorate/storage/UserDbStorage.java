package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User create(User user) {
        String sql = """
                INSERT INTO users (email, login, name, birthday)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setObject(4, user.getBirthday());

            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException(
                    "Не удалось получить id пользователя после сохранения"
            );
        }

        user.setId(keyHolder.getKey().longValue());

        return user;
    }

    @Override
    public User update(User user) {
        String sql = """
                UPDATE users
                SET email = ?,
                    login = ?,
                    name = ?,
                    birthday = ?
                WHERE user_id = ?
                """;

        jdbcTemplate.update(
                sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId()
        );

        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = """
                SELECT user_id,
                       email,
                       login,
                       name,
                       birthday
                FROM users
                WHERE user_id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    User user = new User();

                    user.setId(rs.getLong("user_id"));
                    user.setEmail(rs.getString("email"));
                    user.setLogin(rs.getString("login"));
                    user.setName(rs.getString("name"));
                    user.setBirthday(
                            rs.getDate("birthday").toLocalDate()
                    );

                    return user;
                },
                id
        ).stream().findFirst();
    }

    @Override
    public List<User> findAll() {
        String sql = """
                SELECT user_id,
                       email,
                       login,
                       name,
                       birthday
                FROM users
                ORDER BY user_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = new User();

            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));
            user.setBirthday(
                    rs.getDate("birthday").toLocalDate()
            );

            return user;
        });
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = """
                MERGE INTO friends (user_id, friend_id)
                KEY (user_id, friend_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = """
                DELETE FROM friends
                WHERE user_id = ? AND friend_id = ?
                """;

        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> findFriends(Long userId) {
        String sql = """
                SELECT u.user_id,
                       u.email,
                       u.login,
                       u.name,
                       u.birthday
                FROM users u
                JOIN friends f ON u.user_id = f.friend_id
                WHERE f.user_id = ?
                ORDER BY u.user_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = new User();

            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));
            user.setBirthday(
                    rs.getDate("birthday").toLocalDate()
            );

            return user;
        }, userId);
    }

    @Override
    public List<User> findCommonFriends(Long userId, Long otherUserId) {
        String sql = """
                SELECT u.user_id,
                       u.email,
                       u.login,
                       u.name,
                       u.birthday
                FROM users u
                JOIN friends f1 ON u.user_id = f1.friend_id
                JOIN friends f2 ON u.user_id = f2.friend_id
                WHERE f1.user_id = ?
                  AND f2.user_id = ?
                ORDER BY u.user_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = new User();

            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));
            user.setBirthday(
                    rs.getDate("birthday").toLocalDate()
            );

            return user;
        }, userId, otherUserId);
    }

    @Override
    public boolean hasFriend(Long userId, Long friendId) {
        String sql = """
            SELECT COUNT(*)
            FROM friends
            WHERE user_id = ? AND friend_id = ?
            """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                userId,
                friendId
        );

        return count != null && count > 0;
    }
}
