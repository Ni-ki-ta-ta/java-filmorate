package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @Qualifier("userDbStorage")
    private final UserStorage userStorage;

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

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + id + " не найден"
                ));
    }

    public void addFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        validateUsers(userId, friendId);

        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        validateUsers(userId, friendId);

        userStorage.removeFriend(userId, friendId);
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
}
