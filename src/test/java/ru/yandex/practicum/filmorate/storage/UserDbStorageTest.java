package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userDbStorage;

    @Test
    void shouldCreateUser() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLogin("test");
        user.setName("Тест");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userDbStorage.create(user);

        assertTrue(
                savedUser.getId() != null,
                "После создания пользователю должен быть присвоен id"
        );
        assertEquals(
                "test@test.com",
                savedUser.getEmail(),
                "Email созданного пользователя должен совпадать"
        );
        assertEquals(
                "test",
                savedUser.getLogin(),
                "Логин созданного пользователя должен совпадать"
        );
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLogin("test");
        user.setName("Тест");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userDbStorage.create(user);

        savedUser.setEmail("new@test.com");
        savedUser.setLogin("newLogin");
        savedUser.setName("Новое имя");

        User updatedUser = userDbStorage.update(savedUser);

        assertEquals(
                "new@test.com",
                updatedUser.getEmail(),
                "Email должен обновиться"
        );
        assertEquals(
                "newLogin",
                updatedUser.getLogin(),
                "Логин должен обновиться"
        );
        assertEquals(
                "Новое имя",
                updatedUser.getName(),
                "Имя должно обновиться"
        );
    }

    @Test
    void shouldFindUserById() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setLogin("test");
        user.setName("Тест");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userDbStorage.create(user);

        Optional<User> foundUser = userDbStorage.findById(savedUser.getId());

        assertTrue(
                foundUser.isPresent(),
                "Созданного пользователя можно найти по id"
        );
        assertEquals(
                savedUser.getId(),
                foundUser.get().getId(),
                "Id найденного пользователя должен совпадать"
        );
        assertEquals(
                "test@test.com",
                foundUser.get().getEmail(),
                "Email найденного пользователя должен совпадать"
        );
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> foundUser = userDbStorage.findById(999999L);

        assertFalse(
                foundUser.isPresent(),
                "Для несуществующего пользователя должен возвращаться Optional.empty()"
        );
    }

    @Test
    void shouldFindAllUsers() {
        User firstUser = new User();
        firstUser.setEmail("first@test.com");
        firstUser.setLogin("first");
        firstUser.setName("Первый");
        firstUser.setBirthday(LocalDate.of(2000, 1, 1));

        User secondUser = new User();
        secondUser.setEmail("second@test.com");
        secondUser.setLogin("second");
        secondUser.setName("Второй");
        secondUser.setBirthday(LocalDate.of(2001, 1, 1));

        userDbStorage.create(firstUser);
        userDbStorage.create(secondUser);

        List<User> users = userDbStorage.findAll();

        assertEquals(
                2,
                users.size(),
                "В базе должно быть два созданных пользователя"
        );
    }
}
