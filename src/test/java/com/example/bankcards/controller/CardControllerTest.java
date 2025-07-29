package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CardSearchRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.card.CardNotFoundException;
import com.example.bankcards.exception.card.CardNotOwnedByUserException;
import com.example.bankcards.exception.card.CardStatusAlreadySetException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.security.JwtAuthenticationEntryPoint;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.security.JwtUserDetailsService;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID cardId;
    private UUID ownerId;
    private CreateCardRequest createCardRequest;
    private CardDto cardDto;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        createCardRequest = new CreateCardRequest(
                "1234567890123456",
                ownerId,
                LocalDate.now().plusYears(1),
                BigDecimal.valueOf(100.00)
        );
        cardDto = new CardDto(
                cardId,
                "************3456",
                ownerId,
                LocalDate.now().plusYears(1),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(100.00)
        );
    }

    @Test
    void createCard_validRequest_shouldReturnCreated() throws Exception {
        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(cardDto);

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.maskedCardNumber").value("************3456"));
    }

    @Test
    void createCard_userNotFound_shouldReturnBadRequest() throws Exception {
        when(cardService.createCard(any(CreateCardRequest.class)))
                .thenThrow(new UserNotFoundException(ownerId));

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCard_invalidCardNumber_shouldReturnBadRequest() throws Exception {
        CreateCardRequest invalidRequest = new CreateCardRequest(
                "123",
                ownerId,
                LocalDate.now().plusYears(1),
                BigDecimal.valueOf(100.00)
        );

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCards_noParams_shouldReturnOkAndListOfCards() throws Exception {
        when(cardService.getAllCards(any(), any(), anyBoolean(), any(CardSearchRequest.class)))
                .thenReturn(List.of(cardDto));

        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cardId.toString()));
    }

    @Test
    void getAllCardsForUser_validUser_shouldReturnOkAndListOfCards() throws Exception {
        when(cardService.getAllCardsForUser(eq(ownerId), any(), any(), anyBoolean()))
                .thenReturn(List.of(cardDto));

        mockMvc.perform(get("/api/v1/cards/user/{userId}", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cardId.toString()));
    }

    @Test
    void getAllCardsForUser_userNotFound_shouldReturnNotFound() throws Exception {
        when(cardService.getAllCardsForUser(eq(ownerId), any(), any(), anyBoolean()))
                .thenThrow(new UserNotFoundException(ownerId));

        mockMvc.perform(get("/api/v1/cards/user/{userId}", ownerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardById_existingCard_shouldReturnOkAndCardDto() throws Exception {
        when(cardService.getCardDtoById(eq(cardId))).thenReturn(cardDto);

        mockMvc.perform(get("/api/v1/cards/{cardId}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()));
    }

    @Test
    void getCardById_nonExistingCard_shouldReturnNotFound() throws Exception {
        when(cardService.getCardDtoById(eq(cardId))).thenThrow(new CardNotFoundException(cardId));

        mockMvc.perform(get("/api/v1/cards/{cardId}", cardId))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCardStatus_validRequest_shouldReturnNoContent() throws Exception {
        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        doNothing().when(cardService).changeCardStatus(eq(cardId), any(UpdateCardStatusRequest.class));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/status", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void changeCardStatus_cardNotFound_shouldReturnNotFound() throws Exception {
        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        doThrow(new CardNotFoundException(cardId)).when(cardService)
                .changeCardStatus(eq(cardId), any(UpdateCardStatusRequest.class));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/status", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeCardStatus_statusAlreadySet_shouldReturnConflict() throws Exception {
        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        doThrow(new CardStatusAlreadySetException(cardId)).when(cardService)
                .changeCardStatus(eq(cardId), any(UpdateCardStatusRequest.class));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/status", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void userBlockCard_validRequest_shouldReturnNoContent() throws Exception {
        doNothing().when(cardService).userBlockCard(eq(cardId));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/block", cardId))
                .andExpect(status().isNoContent());
    }

    @Test
    void userBlockCard_cardNotFound_shouldReturnNotFound() throws Exception {
        doThrow(new CardNotFoundException(cardId)).when(cardService).userBlockCard(eq(cardId));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/block", cardId))
                .andExpect(status().isNotFound());
    }

    @Test
    void userBlockCard_cardNotOwned_shouldReturnNotFound() throws Exception {
        doThrow(new CardNotOwnedByUserException(cardId, ownerId)).when(cardService).userBlockCard(eq(cardId));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/block", cardId))
                .andExpect(status().isNotFound());
    }

    @Test
    void userBlockCard_statusAlreadySet_shouldReturnConflict() throws Exception {
        doThrow(new CardStatusAlreadySetException(cardId)).when(cardService).userBlockCard(eq(cardId));

        mockMvc.perform(patch("/api/v1/cards/{cardId}/block", cardId))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteCard_existingCard_shouldReturnNoContent() throws Exception {
        doNothing().when(cardService).deleteCard(eq(cardId));

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCard_nonExistingCard_shouldReturnNotFound() throws Exception {
        doThrow(new CardNotFoundException(cardId)).when(cardService).deleteCard(eq(cardId));

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId))
                .andExpect(status().isNotFound());
    }
}