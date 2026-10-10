package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private static final String SELECT_REVIEWS = """
            SELECT r.review_id,
                   r.content,
                   r.is_positive,
                   r.user_id,
                   r.film_id,
                   COALESCE(SUM(CASE
                                    WHEN rl.is_useful IS NULL THEN 0
                                    WHEN rl.is_useful THEN 1
                                    ELSE -1
                                END), 0) AS useful
            FROM reviews r
            LEFT JOIN review_likes rl ON r.review_id = rl.review_id
            """;

    private static final String GROUP_BY = """
            GROUP BY r.review_id, r.content, r.is_positive, r.user_id, r.film_id
            """;

    private static final String ORDER_AND_LIMIT = """
            ORDER BY useful DESC, r.review_id
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review create(Review review) {
        String sql = """
                INSERT INTO reviews (content, is_positive, user_id, film_id)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());

            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("Не удалось получить id отзыва");
        }

        Long id = keyHolder.getKey().longValue();

        return findById(id).orElseThrow(() ->
                new IllegalStateException("Созданный отзыв не найден"));
    }

    @Override
    public Review update(Review review) {
        String sql = """
                UPDATE reviews
                SET content = ?,
                    is_positive = ?
                WHERE review_id = ?
                """;

        jdbcTemplate.update(
                sql,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId()
        );

        return findById(review.getReviewId()).orElseThrow(() ->
                new IllegalStateException("Обновлённый отзыв не найден"));
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id = ?", id);
    }

    @Override
    public Optional<Review> findById(Long id) {
        String sql = SELECT_REVIEWS
                + "WHERE r.review_id = ?\n"
                + GROUP_BY;

        List<Review> reviews = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapReview(rs),
                id
        );

        return reviews.stream().findFirst();
    }

    @Override
    public List<Review> findByFilmId(Long filmId, int count) {
        if (filmId == null) {
            String sql = SELECT_REVIEWS + GROUP_BY + ORDER_AND_LIMIT;

            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> mapReview(rs),
                    count
            );
        }

        String sql = SELECT_REVIEWS
                + "WHERE r.film_id = ?\n"
                + GROUP_BY
                + ORDER_AND_LIMIT;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapReview(rs),
                filmId,
                count
        );
    }

    @Override
    public void addReaction(Long reviewId, Long userId, boolean useful) {
        String sql = """
                MERGE INTO review_likes (review_id, user_id, is_useful)
                KEY (review_id, user_id)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.update(sql, reviewId, userId, useful);
    }

    @Override
    public void removeReaction(Long reviewId, Long userId, boolean useful) {
        String sql = """
                DELETE FROM review_likes
                WHERE review_id = ? AND user_id = ? AND is_useful = ?
                """;

        jdbcTemplate.update(sql, reviewId, userId, useful);
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review review = new Review();

        review.setReviewId(rs.getLong("review_id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getLong("user_id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUseful(rs.getInt("useful"));

        return review;
    }
}
