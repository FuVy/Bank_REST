package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class UpdateCardStatusRequest {
    @NotNull(message = "New status can't be null.")
    CardStatus newStatus;
}
