package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    private long nextId = 1;

    @GetMapping
    public List<User> findAll() {
        log.info("Получен список пользователей");
        return new ArrayList<>(users.values());
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        validateUser(user);

        setUserName(user);

        user.setId(nextId++);
        users.put(user.getId(), user);

        log.info("Добавлен пользователь: {}", user);

        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        validateUser(user);

        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.error("Пользователь с id {} не найден", user.getId());
            throw new NotFoundException("Пользователь не найден");
        }

        setUserName(user);

        users.put(user.getId(), user);

        log.info("Обновлен пользователь: {}", user);

        return user;
    }

    private void validateUser(User user) {
        if (user.getLogin().contains(" ")) {
            log.error("Логин содержит пробелы");
            throw new ValidationException("Логин не может содержать пробелы");
        }
    }

    private void setUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}