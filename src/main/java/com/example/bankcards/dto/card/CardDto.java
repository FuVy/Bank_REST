package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
public class CardDto {
    UUID id;
    String maskedCardNumber;
    UUID ownerId;
    LocalDate expiryDate;
    CardStatus status;
    BigDecimal balance;
}