package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:filmorate_test;DB_CLOSE_DELAY=-1",
        "spring.sql.init.mode=always"
})
class DeleteEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long createUser() throws Exception {
        String json = """
                {
                    "email": "delete-test@example.com",
                    "login": "delete_test",
                    "name": "Test User",
                    "birthday": "2000-01-01"
                }
                """;

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode user = objectMapper.readTree(response);
        return user.get("id").asLong();
    }

    private long createFilm() throws Exception {
        String json = """
                {
                    "name": "Delete Test Film",
                    "description": "Film for deletion test",
                    "releaseDate": "2020-01-01",
                    "duration": 120,
                    "mpa": {
                        "id": 1
                    }
                }
                """;

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode film = objectMapper.readTree(response);
        return film.get("id").asLong();
    }

    @Test
    void shouldDeleteUser() throws Exception {
        long userId = createUser();

        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ?",
                Integer.class,
                userId
        );
        assertEquals(0, count);
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentUser() throws Exception {
        mockMvc.perform(delete("/users/{userId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteFilm() throws Exception {
        long filmId = createFilm();

        mockMvc.perform(delete("/films/{filmId}", filmId))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM films WHERE film_id = ?",
                Integer.class,
                filmId
        );
        assertEquals(0, count);
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentFilm() throws Exception {
        mockMvc.perform(delete("/films/{filmId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }
}
