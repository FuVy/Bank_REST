package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CardSearchRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;

import java.util.List;
import java.util.UUID;

public interface CardService {
    CardDto createCard(CreateCardRequest request);
    List<CardDto> getAllCards(Integer pageNumber, Integer pageSize, boolean ascendingCreationDate, CardSearchRequest searchRequest);
    List<CardDto> getAllCardsForUser(UUID userId, Integer pageNumber, Integer pageSize, boolean ascendingCreationDate);
    CardDto getCardDtoById(UUID cardId);
    void changeCardStatus(UUID cardId, UpdateCardStatusRequest request);
    void userBlockCard(UUID cardId);
    void deleteCard(UUID cardId);
}
