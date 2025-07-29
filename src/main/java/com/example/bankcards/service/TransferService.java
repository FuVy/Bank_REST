package com.example.bankcards.service;

import com.example.bankcards.dto.card.TransferRequest;

import java.util.UUID;

public interface TransferService {
    void transferBetweenUserOwnedCards(UUID userId, TransferRequest request);
}
