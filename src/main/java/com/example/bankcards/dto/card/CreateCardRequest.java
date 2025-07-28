package com.example.bankcards.dto.card;

import jakarta.validation.constraints.*;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
public class CreateCardRequest {
    @NotBlank(message = "Card number can't be blank.")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be 16 digits.")
    String cardNumber;

    @NotBlank(message = "Owner ID can't be blank.")
    UUID ownerId;

    @NotNull(message = "Expiry date can't be null.")
    @Future(message = "Expiry date must be in the future.")
    LocalDate expiryDate;

    @NotNull(message = "Initial balance can't be null.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance can't be negative.")
    BigDecimal initialBalance;
}
