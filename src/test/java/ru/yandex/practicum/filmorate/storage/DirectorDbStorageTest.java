package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.director.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({DirectorDbStorage.class, DirectorRowMapper.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DirectorDbStorageTest {
    private final DirectorDbStorage directorStorage;
    private final JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateAndReturnDirector() {
        Director director = new Director();
        director.setName("Cristopher Nolan");

        Director created = directorStorage.create(director);
        assertThat(created.getId()).isNotNull();

        Director found = directorStorage.findById(created.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Cristopher Nolan");
    }

    @Test
    void shouldUpdateDirector() {
        Director director = new Director();
        director.setName("Cristopher Nolan");

        directorStorage.create(director);

        director.setName("Gay Richi");
        directorStorage.update(director);
        Director found = directorStorage.findById(director.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Gay Richi");
    }

    @Test
    void shouldDeleteDirector() {
        Director director = new Director();
        director.setName("Cristopher Nolan");

        Director created = directorStorage.create(director);
        directorStorage.delete(created.getId());

        assertThat(directorStorage.findById(created.getId())).isEmpty();
    }

    @Test
    void shouldGetAllDirectors() {
        Director director1 = new Director();
        director1.setName("Cristopher Nolan");
        directorStorage.create(director1);

        Director director2 = new Director();
        director2.setName("Gay Richi");
        directorStorage.create(director2);

        List<Director> directors = directorStorage.findAll();

        assertThat(directors)
                .extracting(Director::getId)
                .containsExactlyInAnyOrder(director1.getId(), director2.getId());
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
        assertThat(directorStorage.findById(-1)).isEmpty();
    }
}
