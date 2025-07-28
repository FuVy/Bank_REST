package com.example.bankcards.dto.user;

import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
public class BalanceDto {
    UUID userId;
    BigDecimal totalBalance;
}
