package com.example.userservice.controller;

import com.example.userservice.dto.user.UserCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.userservice.dto.card.CardCreateRequest;
import com.example.userservice.dto.card.CardUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CardControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createUser(String email) throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), email);
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createCard(Long userId, String number) throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                number, "ANNA IVANOVA", LocalDate.of(2028, 12, 1));
        String response = mockMvc.perform(post("/api/v1/users/" + userId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void createCard_success() throws Exception {
        Long userId = createUser("card.create@example.com");

        CardCreateRequest request = new CardCreateRequest(
                "1111222233334444", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post("/api/v1/users/" + userId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holder").value("ANNA IVANOVA"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createCard_maxCards_returnsUnprocessableEntity() throws Exception {
        Long userId = createUser("card.maxcards@example.com");
        String[] numbers = {
            "1000000000000001", "1000000000000002", "1000000000000003",
            "1000000000000004", "1000000000000005"
        };
        for (String number : numbers) {
            createCard(userId, number);
        }

        CardCreateRequest sixth = new CardCreateRequest(
                "1000000000000006", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post("/api/v1/users/" + userId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sixth)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createCard_inactiveUser_returnsUnprocessableEntity() throws Exception {
        Long userId = createUser("card.inactive@example.com");
        mockMvc.perform(patch("/api/v1/users/" + userId + "/deactivate"))
                .andExpect(status().isNoContent());

        CardCreateRequest request = new CardCreateRequest(
                "2222333344445555", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post("/api/v1/users/" + userId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getCardsByUserId_returnsPaginatedResults() throws Exception {
        Long userId = createUser("card.list@example.com");
        createCard(userId, "3333444455556661");
        createCard(userId, "3333444455556662");

        mockMvc.perform(get("/api/v1/users/" + userId + "/cards")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getCardsByUserId_filterByHolder_returnsMatchingCards() throws Exception {
        Long userId = createUser("card.holder.filter@example.com");

        CardCreateRequest req1 = new CardCreateRequest(
                "4444555566667771", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));
        CardCreateRequest req2 = new CardCreateRequest(
                "4444555566667772", "BORIS PETROV", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post("/api/v1/users/" + userId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/users/" + userId + "/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users/" + userId + "/cards").param("holder", "ANNA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].holder").value("ANNA IVANOVA"));
    }

    @Test
    void getCardById_success() throws Exception {
        Long userId = createUser("card.getbyid@example.com");
        Long cardId = createCard(userId, "5555666677778881");

        mockMvc.perform(get("/api/v1/users/" + userId + "/cards/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));
    }

    @Test
    void getCardById_notFound_returns404() throws Exception {
        Long userId = createUser("card.notfound@example.com");

        mockMvc.perform(get("/api/v1/users/" + userId + "/cards/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCard_success() throws Exception {
        Long userId = createUser("card.update@example.com");
        Long cardId = createCard(userId, "6666777788889991");

        CardUpdateRequest updateRequest = new CardUpdateRequest("UPDATED HOLDER", null);

        mockMvc.perform(put("/api/v1/users/" + userId + "/cards/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("UPDATED HOLDER"));
    }

    @Test
    void deactivateCard_success() throws Exception {
        Long userId = createUser("card.deactivate@example.com");
        Long cardId = createCard(userId, "7777888899990001");

        mockMvc.perform(patch("/api/v1/users/" + userId + "/cards/" + cardId + "/deactivate"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/" + userId + "/cards/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activateCard_success() throws Exception {
        Long userId = createUser("card.activate@example.com");
        Long cardId = createCard(userId, "8888999900001111");

        mockMvc.perform(patch("/api/v1/users/" + userId + "/cards/" + cardId + "/deactivate"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/users/" + userId + "/cards/" + cardId + "/activate"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/" + userId + "/cards/" + cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }
}
