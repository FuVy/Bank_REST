package com.example.bankcards.controller;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.exception.card.CardNotOwnedByUserException;
import com.example.bankcards.exception.card.InvalidCardOperationException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.security.JwtAuthenticationEntryPoint;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.security.JwtUserDetailsService;
import com.example.bankcards.service.TransferService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

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

    private UUID userId;
    private UUID fromCardId;
    private UUID toCardId;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fromCardId = UUID.randomUUID();
        toCardId = UUID.randomUUID();
        transferRequest = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100.00));
    }

    @Test
    void transferBetweenUserOwnedCards_validRequest_shouldReturnNoContent() throws Exception {
        doNothing().when(transferService).transferBetweenUserOwnedCards(eq(userId), any(TransferRequest.class));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    void transferBetweenUserOwnedCards_userNotFound_shouldReturnNotFound() throws Exception {
        doThrow(new UserNotFoundException(userId)).when(transferService)
                .transferBetweenUserOwnedCards(eq(userId), any(TransferRequest.class));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transferBetweenUserOwnedCards_cardNotOwned_shouldReturnNotFound() throws Exception {
        doThrow(new CardNotOwnedByUserException(fromCardId, userId)).when(transferService)
                .transferBetweenUserOwnedCards(eq(userId), any(TransferRequest.class));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transferBetweenUserOwnedCards_invalidCardOperation_shouldReturnBadRequest() throws Exception {
        doThrow(new InvalidCardOperationException("Insufficient balance")).when(transferService)
                .transferBetweenUserOwnedCards(eq(userId), any(TransferRequest.class));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenUserOwnedCards_missingFromCardId_shouldReturnBadRequest() throws Exception {
        TransferRequest invalidRequest = new TransferRequest(null, toCardId, BigDecimal.valueOf(100.00));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenUserOwnedCards_missingToCardId_shouldReturnBadRequest() throws Exception {
        TransferRequest invalidRequest = new TransferRequest(fromCardId, null, BigDecimal.valueOf(100.00));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenUserOwnedCards_negativeAmount_shouldReturnBadRequest() throws Exception {
        TransferRequest invalidRequest = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(-10.00));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenUserOwnedCards_zeroAmount_shouldReturnBadRequest() throws Exception {
        TransferRequest invalidRequest = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(0.00));

        mockMvc.perform(post("/api/v1/transfers/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}