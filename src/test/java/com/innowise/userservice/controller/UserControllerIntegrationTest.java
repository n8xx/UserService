package com.innowise.userservice.controller;

import com.innowise.userservice.dto.user.UserCreateRequest;
import com.innowise.userservice.dto.user.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String USERS_API = "/api/v1/users";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";
    private static final String ADMIN_USERNAME = "1";
    private static final String REGULAR_USER_USERNAME = "2";

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void createUser_success() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "anna.integration@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Anna"))
                .andExpect(jsonPath("$.email").value("anna.integration@innowise.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createUser_withoutToken_returns401() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "no.token@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = REGULAR_USER_USERNAME, roles = ROLE_USER)
    void createUser_withUserRole_returns403() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "user.role@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void getUserById_notFound() throws Exception {
        mockMvc.perform(get(USERS_API + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    @Test
    void getUserById_userAccessOwnProfile() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Own", "Profile", LocalDate.of(1995, 5, 15), "own.profile@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .with(user(ADMIN_USERNAME).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = extractId(response);

        mockMvc.perform(get(USERS_API + "/" + userId)
                        .with(user(userId.toString()).roles(ROLE_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Own"));
    }

    @Test
    void getUserById_userAccessOtherProfile_returns403() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Other", "User", LocalDate.of(1995, 5, 15), "other.user@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .with(user(ADMIN_USERNAME).roles(ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = extractId(response);

        mockMvc.perform(get(USERS_API + "/" + userId)
                        .with(user(REGULAR_USER_USERNAME).roles(ROLE_USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "invalid-email");

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void createUser_duplicateEmail_returnsError() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "duplicate@innowise.com");

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void updateUser_success() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "update.test@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = extractId(response);
        UserUpdateRequest updateRequest = new UserUpdateRequest("NewName", null, null);

        mockMvc.perform(put(USERS_API + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void getAllUsers_adminAccess() throws Exception {
        mockMvc.perform(get(USERS_API)
                        .param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(username = REGULAR_USER_USERNAME, roles = ROLE_USER)
    void getAllUsers_userRole_returns403() throws Exception {
        mockMvc.perform(get(USERS_API))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = ROLE_ADMIN)
    void deactivateUser_success() throws Exception {
        UserCreateRequest createRequest = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), "deactivate.test@innowise.com");

        String response = mockMvc.perform(post(USERS_API)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = extractId(response);

        mockMvc.perform(patch(USERS_API + "/" + id + "/deactivate"))
                .andExpect(status().isNoContent());
    }
}