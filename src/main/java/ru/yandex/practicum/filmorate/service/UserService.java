package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

    private final EventStorage eventStorage;

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        validateUser(user);
        setUserName(user);

        return userStorage.create(user);
    }

    public User update(User user) {
        validateUser(user);

        if (user.getId() == null || userStorage.findById(user.getId()).isEmpty()) {
            throw new NotFoundException("Пользователь не найден");
        }

        setUserName(user);

        return userStorage.update(user);
    }

    public void delete(Long userId) {
        findById(userId);
        userStorage.delete(userId);
    }

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + id + " не найден"
                ));
    }

    public void addFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);

        if (!userStorage.hasFriend(userId, friendId)) {
            userStorage.addFriend(userId, friendId);

            eventStorage.addEvent(new Event(
                    null,
                    System.currentTimeMillis(),
                    userId,
                    EventType.FRIEND,
                    Operation.ADD,
                    friendId
            ));
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);

        if (userStorage.hasFriend(userId, friendId)) {
            userStorage.removeFriend(userId, friendId);

            eventStorage.addEvent(new Event(
                    null,
                    System.currentTimeMillis(),
                    userId,
                    EventType.FRIEND,
                    Operation.REMOVE,
                    friendId
            ));
        }
    }

    public List<User> getFriends(Long userId) {
        findById(userId);

        return userStorage.findFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        findById(userId);
        findById(otherId);
        validateUsers(userId, otherId);

        return userStorage.findCommonFriends(userId, otherId);
    }

    private void validateUser(User user) {
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы");
        }
    }

    private void setUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private void validateUsers(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException(
                    "Пользователь не может добавить сам себя в друзья"
            );
        }
    }

    public List<Film> getRecommendations(Long userId) {
        findById(userId);

        List<User> users = userStorage.findAll();
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        var likedFilmsByUser = filmStorage.findLikedFilmIdsByUserIds(userIds);
        Set<Long> userLikedFilms = new HashSet<>(
                likedFilmsByUser.getOrDefault(userId, List.of())
        );

        if (userLikedFilms.isEmpty()) {
            return List.of();
        }

        Long similarUserId = null;
        int maxCommonLikes = 0;

        for (User otherUser : users) {
            if (otherUser.getId().equals(userId)) {
                continue;
            }

            Set<Long> otherUserLikedFilms = new HashSet<>(
                    likedFilmsByUser.getOrDefault(otherUser.getId(), List.of())
            );

            Set<Long> commonFilms = new HashSet<>(userLikedFilms);
            commonFilms.retainAll(otherUserLikedFilms);

            if (commonFilms.size() > maxCommonLikes) {
                maxCommonLikes = commonFilms.size();
                similarUserId = otherUser.getId();
            }
        }

        if (similarUserId == null) {
            return List.of();
        }

        Set<Long> recommendedFilmIds = new HashSet<>(
                likedFilmsByUser.getOrDefault(similarUserId, List.of())
        );
        recommendedFilmIds.removeAll(userLikedFilms);

        return filmStorage.findAll().stream()
                .filter(film -> recommendedFilmIds.contains(film.getId()))
                .toList();
    }
}
