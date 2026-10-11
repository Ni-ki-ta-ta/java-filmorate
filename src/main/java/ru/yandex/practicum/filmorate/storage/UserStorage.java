package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {

    User create(User user);

    User update(User user);

    Optional<User> findById(Long id);

    void delete(Long id);

    List<User> findAll();

    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    List<User> findFriends(Long userId);

    List<User> findCommonFriends(Long userId, Long otherUserId);
}
