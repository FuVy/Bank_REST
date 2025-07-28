package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import lombok.Value;

import java.time.LocalDate;

@Value
public class CardSearchRequest {
    CardStatus status;
    LocalDate expiryDate;
}
