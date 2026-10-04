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
        User user = createUser(
                "test@test.com",
                "test",
                "Тест"
        );

        User savedUser = userDbStorage.create(user);

        assertNotNull(
                savedUser.getId(),
                "После создания пользователю должен быть присвоен id"
        );
        assertEquals(
                "test@test.com",
                savedUser.getEmail()
        );
        assertEquals(
                "test",
                savedUser.getLogin()
        );
    }

    @Test
    void shouldUpdateUser() {
        User user = userDbStorage.create(
                createUser(
                        "test@test.com",
                        "test",
                        "Тест"
                )
        );

        user.setEmail("new@test.com");
        user.setLogin("newLogin");
        user.setName("Новое имя");

        User updatedUser = userDbStorage.update(user);

        assertEquals(
                "new@test.com",
                updatedUser.getEmail()
        );
        assertEquals(
                "newLogin",
                updatedUser.getLogin()
        );
        assertEquals(
                "Новое имя",
                updatedUser.getName()
        );

        Optional<User> foundUser =
                userDbStorage.findById(user.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(
                "new@test.com",
                foundUser.get().getEmail()
        );
    }

    @Test
    void shouldFindUserById() {
        User user = userDbStorage.create(
                createUser(
                        "test@test.com",
                        "test",
                        "Тест"
                )
        );

        Optional<User> foundUser =
                userDbStorage.findById(user.getId());

        assertTrue(
                foundUser.isPresent(),
                "Созданного пользователя можно найти по id"
        );
        assertEquals(
                user.getId(),
                foundUser.get().getId()
        );
        assertEquals(
                "test@test.com",
                foundUser.get().getEmail()
        );
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> foundUser =
                userDbStorage.findById(999999L);

        assertFalse(
                foundUser.isPresent(),
                "Для несуществующего пользователя должен возвращаться Optional.empty()"
        );
    }

    @Test
    void shouldFindAllUsers() {
        userDbStorage.create(
                createUser(
                        "first@test.com",
                        "first",
                        "Первый"
                )
        );

        userDbStorage.create(
                createUser(
                        "second@test.com",
                        "second",
                        "Второй"
                )
        );

        List<User> users = userDbStorage.findAll();

        assertEquals(
                2,
                users.size(),
                "В базе должно быть два созданных пользователя"
        );
    }

    @Test
    void shouldAddFriend() {
        User user = userDbStorage.create(
                createUser(
                        "user@test.com",
                        "user",
                        "Пользователь"
                )
        );

        User friend = userDbStorage.create(
                createUser(
                        "friend@test.com",
                        "friend",
                        "Друг"
                )
        );

        userDbStorage.addFriend(
                user.getId(),
                friend.getId()
        );

        List<User> friends =
                userDbStorage.findFriends(user.getId());

        assertEquals(
                1,
                friends.size(),
                "После добавления должен появиться один друг"
        );
        assertEquals(
                friend.getId(),
                friends.get(0).getId(),
                "В списке должен быть добавленный друг"
        );
    }

    @Test
    void shouldRemoveFriend() {
        User user = userDbStorage.create(
                createUser(
                        "user@test.com",
                        "user",
                        "Пользователь"
                )
        );

        User friend = userDbStorage.create(
                createUser(
                        "friend@test.com",
                        "friend",
                        "Друг"
                )
        );

        userDbStorage.addFriend(
                user.getId(),
                friend.getId()
        );

        userDbStorage.removeFriend(
                user.getId(),
                friend.getId()
        );

        List<User> friends =
                userDbStorage.findFriends(user.getId());

        assertTrue(
                friends.isEmpty(),
                "После удаления список друзей должен быть пустым"
        );
    }

    @Test
    void shouldFindFriends() {
        User user = userDbStorage.create(
                createUser(
                        "user@test.com",
                        "user",
                        "Пользователь"
                )
        );

        User firstFriend = userDbStorage.create(
                createUser(
                        "first@test.com",
                        "first",
                        "Первый друг"
                )
        );

        User secondFriend = userDbStorage.create(
                createUser(
                        "second@test.com",
                        "second",
                        "Второй друг"
                )
        );

        userDbStorage.addFriend(
                user.getId(),
                firstFriend.getId()
        );

        userDbStorage.addFriend(
                user.getId(),
                secondFriend.getId()
        );

        List<User> friends =
                userDbStorage.findFriends(user.getId());

        assertEquals(
                2,
                friends.size(),
                "Должны вернуться два друга"
        );

        assertEquals(
                firstFriend.getId(),
                friends.get(0).getId()
        );

        assertEquals(
                secondFriend.getId(),
                friends.get(1).getId()
        );
    }

    @Test
    void shouldFindCommonFriends() {
        User firstUser = userDbStorage.create(
                createUser(
                        "first@test.com",
                        "first",
                        "Первый"
                )
        );

        User secondUser = userDbStorage.create(
                createUser(
                        "second@test.com",
                        "second",
                        "Второй"
                )
        );

        User commonFriend = userDbStorage.create(
                createUser(
                        "common@test.com",
                        "common",
                        "Общий друг"
                )
        );

        User onlyFirstFriend = userDbStorage.create(
                createUser(
                        "onlyfirst@test.com",
                        "onlyfirst",
                        "Друг первого"
                )
        );

        userDbStorage.addFriend(
                firstUser.getId(),
                commonFriend.getId()
        );

        userDbStorage.addFriend(
                firstUser.getId(),
                onlyFirstFriend.getId()
        );

        userDbStorage.addFriend(
                secondUser.getId(),
                commonFriend.getId()
        );

        List<User> commonFriends =
                userDbStorage.findCommonFriends(
                        firstUser.getId(),
                        secondUser.getId()
                );

        assertEquals(
                1,
                commonFriends.size(),
                "Должен найтись один общий друг"
        );

        assertEquals(
                commonFriend.getId(),
                commonFriends.get(0).getId()
        );
    }

    @Test
    void shouldKeepFriendshipOneSided() {
        User user = userDbStorage.create(
                createUser(
                        "user@test.com",
                        "user",
                        "Пользователь"
                )
        );

        User friend = userDbStorage.create(
                createUser(
                        "friend@test.com",
                        "friend",
                        "Друг"
                )
        );

        userDbStorage.addFriend(
                user.getId(),
                friend.getId()
        );

        assertEquals(
                1,
                userDbStorage.findFriends(user.getId()).size()
        );

        assertTrue(
                userDbStorage.findFriends(friend.getId()).isEmpty(),
                "Дружба должна быть односторонней"
        );
    }

    private User createUser(
            String email,
            String login,
            String name
    ) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
