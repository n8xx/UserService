package com.innowise.userservice.controller;

import com.innowise.userservice.dto.user.UserCreateRequest;
import com.innowise.userservice.dto.user.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String USERS_API = "/api/v1/users";
    private static final String AUTHORIZATION = "Authorization";

    @Test
    void createUser_success() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "anna.integration@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.email").value("anna.integration@innowise.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createUser_withoutToken_isUnauthenticated() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "no.token@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createUser_withUserRole_returns403() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "user.role@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, userToken(REGULAR_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_notFound() throws Exception {
        mockMvc.perform(get(USERS_API + "/999")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    @Test
    void getUserById_userAccessOwnProfile() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Own", "Profile", LocalDate.of(1995, 5, 15), "own.profile@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = extractId(response);

        mockMvc.perform(get(USERS_API + "/" + userId)
                        .header(AUTHORIZATION, userToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Own"));
    }

    @Test
    void getUserById_userAccessOtherProfile_returns403() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Other", "User", LocalDate.of(1995, 5, 15), "other.user@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = extractId(response);

        mockMvc.perform(get(USERS_API + "/" + userId)
                        .header(AUTHORIZATION, userToken(REGULAR_USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "invalid-email");

        mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    void createUser_duplicateEmail_returnsError() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "duplicate@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateUser_success() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "update.test@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = extractId(response);
        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        mockMvc.perform(put(USERS_API + "/" + id)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void getAllUsers_adminAccess() throws Exception {
        mockMvc.perform(get(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void getAllUsers_userRole_returns403() throws Exception {
        mockMvc.perform(get(USERS_API)
                        .header(AUTHORIZATION, userToken(REGULAR_USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateUser_success() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "deactivate.test@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = extractId(response);

        mockMvc.perform(patch(USERS_API + "/" + id + "/deactivate")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNoContent());
    }
}