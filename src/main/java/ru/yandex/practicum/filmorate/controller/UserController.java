package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> findAll() {
        log.info("Получен список пользователей");
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        log.info("Получен пользователь с id {}", id);
        return userService.findById(id);
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        User createdUser = userService.create(user);
        log.info("Добавлен пользователь: {}", createdUser);
        return createdUser;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        User updatedUser = userService.update(user);
        log.info("Обновлён пользователь: {}", updatedUser);
        return updatedUser;
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(
            @PathVariable Long id,
            @PathVariable Long friendId
    ) {
        log.info(
                "Пользователь {} добавил в друзья пользователя {}",
                id,
                friendId
        );

        userService.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(
            @PathVariable Long id,
            @PathVariable Long friendId
    ) {
        log.info(
                "Пользователь {} удалил из друзей пользователя {}",
                id,
                friendId
        );

        userService.removeFriend(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Long id) {
        log.info("Получен список друзей пользователя {}", id);

        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(
            @PathVariable Long id,
            @PathVariable Long otherId
    ) {
        log.info(
                "Получен список общих друзей пользователей {} и {}",
                id,
                otherId
        );

        return userService.getCommonFriends(id, otherId);
    }

    @GetMapping("/{id}/recommendations")
    public List<Film> getRecommendations(@PathVariable Long id) {
        return userService.getRecommendations(id);
    }
}
