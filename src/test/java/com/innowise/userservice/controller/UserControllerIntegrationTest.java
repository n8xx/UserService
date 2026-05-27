package com.example.userservice.controller;

import com.example.userservice.dto.user.UserCreateRequest;
import com.example.userservice.dto.user.UserUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class UserControllerIntegrationTest {


    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");


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

    @Test
    void createUser_success() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "anna.integration@example.com"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.email").value("anna.integration@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getUserById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    @Test
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "invalid-email"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void createUser_duplicateEmail_returnsError() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "duplicate@example.com"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateUser_success() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "update.test@example.com"
        );

        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        mockMvc.perform(put("/api/v1/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void getAllUsers_filterByName_returnsMatchingUsers() throws Exception {
        UserCreateRequest anna = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "filter.anna@example.com");
        UserCreateRequest boris = new UserCreateRequest(
                "Boris", "Ivanov", LocalDate.of(1990, 3, 10), "filter.boris@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anna)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boris)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users").param("name", "Anna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.containsStringIgnoringCase("Anna"))))
                .andExpect(jsonPath("$.content[*].email").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("filter.boris@example.com"))));
    }

    @Test
    void getAllUsers_filterBySurname_returnsMatchingUsers() throws Exception {
        UserCreateRequest carla = new UserCreateRequest(
                "Carla", "Smith", LocalDate.of(1992, 7, 20), "filter.carla@example.com");
        UserCreateRequest david = new UserCreateRequest(
                "David", "Johnson", LocalDate.of(1988, 1, 5), "filter.david@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(carla)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(david)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users").param("surname", "Smith"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].surname").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.containsStringIgnoringCase("Smith"))))
                .andExpect(jsonPath("$.content[*].email").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("filter.david@example.com"))));
    }

    @Test
    void getAllUsers_noFilters_returnsPaginatedResults() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void deactivateUser_success() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Anna",
                "Ivanova",
                LocalDate.of(1995, 5, 15),
                "deactivate.test@example.com"
        );

        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/v1/users/" + id + "/deactivate"))
                .andExpect(status().isNoContent());
    }
}