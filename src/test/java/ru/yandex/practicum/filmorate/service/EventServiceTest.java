
package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private EventStorage eventStorage;
    private UserStorage userStorage;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventStorage = mock(EventStorage.class);
        userStorage = mock(UserStorage.class);
        eventService = new EventService(eventStorage, userStorage);
    }

    @Test
    void shouldReturnUserFeed() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Event event = new Event(
                1L,
                1000L,
                userId,
                EventType.LIKE,
                Operation.ADD,
                10L
        );

        when(userStorage.findById(userId)).thenReturn(Optional.of(user));
        when(eventStorage.findByUserId(userId)).thenReturn(List.of(event));

        List<Event> result = eventService.getFeed(userId);

        assertEquals(
                1,
                result.size(),
                "Лента должна содержать одно событие"
        );
        assertEquals(
                event,
                result.get(0),
                "Лента должна содержать событие, возвращённое хранилищем"
        );
        verify(eventStorage).findByUserId(userId);
    }

    @Test
    void shouldReturnEmptyFeed() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        when(userStorage.findById(userId)).thenReturn(Optional.of(user));
        when(eventStorage.findByUserId(userId)).thenReturn(List.of());

        List<Event> result = eventService.getFeed(userId);

        assertTrue(
                result.isEmpty(),
                "Лента должна быть пустой, если у пользователя нет событий"
        );
        verify(eventStorage).findByUserId(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        Long userId = 99L;

        when(userStorage.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> eventService.getFeed(userId),
                "Для несуществующего пользователя должно выбрасываться исключение"
        );

        verify(eventStorage, never()).findByUserId(userId);
    }
}
