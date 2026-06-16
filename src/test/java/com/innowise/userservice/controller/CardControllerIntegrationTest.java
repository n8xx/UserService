package com.innowise.userservice.controller;

import com.innowise.userservice.dto.card.CardCreateRequest;
import com.innowise.userservice.dto.card.CardUpdateRequest;
import com.innowise.userservice.dto.user.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private static final String USERS_API = "/api/v1/users";
    private static final String AUTHORIZATION = "Authorization";
    private static final Long OTHER_USER_ID = 999L;

    @Test
    void createCard_success() throws Exception {
        Long userId = createUser("card.create@innowise.com");

        CardCreateRequest request = new CardCreateRequest(
                "1111222233334444", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holder").value("ANNA IVANOVA"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createCard_withoutToken_isUnauthenticated() throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                "1111222233334444", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createCard_userAccessOwnCards() throws Exception {
        Long userId = createUser("card.own@innowise.com");

        CardCreateRequest request = new CardCreateRequest(
                "1111222233330001", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, userToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createCard_userAccessOtherCards_returns403() throws Exception {
        Long userId = createUser("card.other@innowise.com");

        CardCreateRequest request = new CardCreateRequest(
                "1111222233330002", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, userToken(OTHER_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_maxCards_returnsUnprocessableEntity() throws Exception {
        Long userId = createUser("card.maxcards@innowise.com");
        String[] numbers = {
                "1000000000000001", "1000000000000002", "1000000000000003",
                "1000000000000004", "1000000000000005"
        };
        for (String number : numbers) {
            createCard(userId, number);
        }

        CardCreateRequest sixth = new CardCreateRequest(
                "1000000000000006", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sixth)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createCard_inactiveUser_returnsUnprocessableEntity() throws Exception {
        Long userId = createUser("card.inactive@innowise.com");
        mockMvc.perform(patch(USERS_API + "/" + userId + "/deactivate")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNoContent());

        CardCreateRequest request = new CardCreateRequest(
                "2222333344445555", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getCardsByUserId_returnsPaginatedResults() throws Exception {
        Long userId = createUser("card.list@innowise.com");
        createCard(userId, "3333444455556661");
        createCard(userId, "3333444455556662");

        mockMvc.perform(get(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getCardsByUserId_filterByHolder_returnsMatchingCards() throws Exception {
        Long userId = createUser("card.holder.filter@innowise.com");

        CardCreateRequest req1 = new CardCreateRequest(
                "4444555566667771", "ANNA IVANOVA", LocalDate.of(2028, 12, 1));
        CardCreateRequest req2 = new CardCreateRequest(
                "4444555566667772", "BORIS PETROV", LocalDate.of(2028, 12, 1));

        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());
        mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .param("holder", "ANNA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].holder").value("ANNA IVANOVA"));
    }

    @Test
    void getCardById_success() throws Exception {
        Long userId = createUser("card.getbyid@innowise.com");
        Long cardId = createCard(userId, "5555666677778881");

        mockMvc.perform(get(cardsApi(userId) + "/" + cardId)
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));
    }

    @Test
    void getCardById_notFound_returns404() throws Exception {
        Long userId = createUser("card.notfound@innowise.com");

        mockMvc.perform(get(cardsApi(userId) + "/999999")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCard_success() throws Exception {
        Long userId = createUser("card.update@innowise.com");
        Long cardId = createCard(userId, "6666777788889991");

        CardUpdateRequest updateRequest = new CardUpdateRequest("UPDATED HOLDER", null);

        mockMvc.perform(put(cardsApi(userId) + "/" + cardId)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("UPDATED HOLDER"));
    }

    @Test
    void deactivateCard_success() throws Exception {
        Long userId = createUser("card.deactivate@innowise.com");
        Long cardId = createCard(userId, "7777888899990001");

        mockMvc.perform(patch(cardsApi(userId) + "/" + cardId + "/deactivate")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(cardsApi(userId) + "/" + cardId)
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void activateCard_success() throws Exception {
        Long userId = createUser("card.activate@innowise.com");
        Long cardId = createCard(userId, "8888999900001111");

        mockMvc.perform(patch(cardsApi(userId) + "/" + cardId + "/deactivate")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch(cardsApi(userId) + "/" + cardId + "/activate")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(cardsApi(userId) + "/" + cardId)
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private String cardsApi(Long userId) {
        return USERS_API + "/" + userId + "/cards";
    }

    private Long createUser(String email) throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Anna", "Ivanova", LocalDate.of(1995, 5, 15), email);
        String response = mockMvc.perform(post(USERS_API)
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extractId(response);
    }

    private Long createCard(Long userId, String number) throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                number, "ANNA IVANOVA", LocalDate.of(2028, 12, 1));
        String response = mockMvc.perform(post(cardsApi(userId))
                        .header(AUTHORIZATION, adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return extractId(response);
    }
}