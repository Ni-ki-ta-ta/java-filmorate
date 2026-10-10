package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Event addEvent(Event event) {
        String sql = """
                INSERT INTO events (
                    timestamp,
                    user_id,
                    event_type,
                    operation,
                    entity_id
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, event.getTimestamp());
            statement.setLong(2, event.getUserId());
            statement.setString(3, event.getEventType().name());
            statement.setString(4, event.getOperation().name());
            statement.setLong(5, event.getEntityId());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            event.setEventId(key.longValue());
        }

        return event;
    }

    @Override
    public List<Event> findByUserId(Long userId) {
        String sql = """
                SELECT event_id,
                       timestamp,
                       user_id,
                       event_type,
                       operation,
                       entity_id
                FROM events
                WHERE user_id = ?
                ORDER BY timestamp DESC, event_id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Event event = new Event();
            event.setEventId(rs.getLong("event_id"));
            event.setTimestamp(rs.getLong("timestamp"));
            event.setUserId(rs.getLong("user_id"));
            event.setEventType(
                    EventType.valueOf(rs.getString("event_type"))
            );
            event.setOperation(
                    Operation.valueOf(rs.getString("operation"))
            );
            event.setEntityId(rs.getLong("entity_id"));
            return event;
        }, userId);
    }
}
