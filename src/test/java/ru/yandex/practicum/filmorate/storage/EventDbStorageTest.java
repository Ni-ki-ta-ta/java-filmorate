package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(EventDbStorage.class)
class EventDbStorageTest {

    @Autowired
    private EventDbStorage eventDbStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveEventAndGenerateId() {
        Long userId = createUser();

        Event event = createEvent(
                userId, 1000L, EventType.LIKE, Operation.ADD, 10L
        );

        Event savedEvent = eventDbStorage.addEvent(event);

        assertNotNull(savedEvent.getEventId(),
                "Событию должен быть присвоен id после сохранения");
        assertEquals(userId, savedEvent.getUserId(),
                "В сохранённом событии должен быть указан пользователь");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM events WHERE event_id = ?",
                Integer.class,
                savedEvent.getEventId()
        );

        assertEquals(1, count,
                "Событие должно сохраниться в базе данных");
    }

    @Test
    void shouldFindEventsWithAllFields() {
        Long userId = createUser();

        Event savedEvent = eventDbStorage.addEvent(
                createEvent(
                        userId, 12345L, EventType.REVIEW,
                        Operation.UPDATE, 25L
                )
        );

        List<Event> events = eventDbStorage.findByUserId(userId);

        assertEquals(1, events.size(),
                "Должно вернуться сохранённое событие пользователя");

        Event foundEvent = events.get(0);

        assertEquals(savedEvent.getEventId(), foundEvent.getEventId(),
                "Id события должен сохраниться");
        assertEquals(12345L, foundEvent.getTimestamp(),
                "Время события должно сохраниться");
        assertEquals(userId, foundEvent.getUserId(),
                "Id пользователя должен сохраниться");
        assertEquals(EventType.REVIEW, foundEvent.getEventType(),
                "Тип события должен сохраниться");
        assertEquals(Operation.UPDATE, foundEvent.getOperation(),
                "Операция должна сохраниться");
        assertEquals(25L, foundEvent.getEntityId(),
                "Id сущности должен сохраниться");
    }

    @Test
    void shouldReturnOnlyEventsOfRequestedUser() {
        Long firstUserId = createUser();
        Long secondUserId = createUser();

        eventDbStorage.addEvent(
                createEvent(firstUserId, 1000L, EventType.LIKE,
                        Operation.ADD, 10L)
        );
        eventDbStorage.addEvent(
                createEvent(secondUserId, 2000L, EventType.FRIEND,
                        Operation.ADD, firstUserId)
        );

        List<Event> events = eventDbStorage.findByUserId(firstUserId);

        assertEquals(1, events.size(),
                "Должны вернуться только события запрошенного пользователя");
        assertEquals(firstUserId, events.get(0).getUserId(),
                "Событие должно принадлежать запрошенному пользователю");
    }

    @Test
    void shouldSortEventsByTimestampAscending() {
        Long userId = createUser();

        Event oldEvent = eventDbStorage.addEvent(
                createEvent(userId, 1000L, EventType.LIKE,
                        Operation.ADD, 10L)
        );
        Event newEvent = eventDbStorage.addEvent(
                createEvent(userId, 3000L, EventType.REVIEW,
                        Operation.ADD, 20L)
        );
        Event middleEvent = eventDbStorage.addEvent(
                createEvent(userId, 2000L, EventType.FRIEND,
                        Operation.REMOVE, 30L)
        );

        List<Event> events = eventDbStorage.findByUserId(userId);

        assertEquals(3, events.size(),
                "Должны вернуться все три события");
        assertEquals(oldEvent.getEventId(), events.get(0).getEventId(),
                "Сначала должно идти самое старое событие");
        assertEquals(middleEvent.getEventId(), events.get(1).getEventId(),
                "Вторым должно идти событие со средним временем");
        assertEquals(newEvent.getEventId(), events.get(2).getEventId(),
                "Последним должно идти самое новое событие");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoEvents() {
        Long userId = createUser();

        List<Event> events = eventDbStorage.findByUserId(userId);

        assertTrue(events.isEmpty(),
                "Для пользователя без событий должна возвращаться пустая лента");
    }

    private Event createEvent(
            Long userId,
            Long timestamp,
            EventType eventType,
            Operation operation,
            Long entityId
    ) {
        return new Event(
                null, timestamp, userId, eventType, operation, entityId
        );
    }

    private Long createUser() {
        String suffix = UUID.randomUUID().toString();
        String email = suffix + "@test.com";

        jdbcTemplate.update(
                """
                        INSERT INTO users (email, login, name, birthday)
                        VALUES (?, ?, ?, ?)
                        """,
                email,
                "user_" + suffix,
                "Тестовый пользователь",
                LocalDate.of(2000, 1, 1)
        );

        return jdbcTemplate.queryForObject(
                "SELECT user_id FROM users WHERE email = ?",
                Long.class,
                email
        );
    }
}
