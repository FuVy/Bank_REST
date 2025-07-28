package com.example.bankcards.dto.card;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
public class TransferRequest {
    @NotNull(message = "Source card ID can't be null.")
    UUID fromCardId;
    @NotNull(message = "Destination card ID can't be null.")
    UUID toCardId;
    @DecimalMin(value = "0.01", message = "Transfer amount must be positive.")
    BigDecimal amount;
}