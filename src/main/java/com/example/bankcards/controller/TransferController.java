package com.example.bankcards.controller;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Operations for transferring money between cards")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "Transfer money between cards owned by a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transfer successful",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid transfer operation (e.g., insufficient balance, inactive card, same card)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User or Card not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not own one or both cards",
                    content = @Content)
    })
    @PostMapping("/user/{userId}")
    public ResponseEntity<Void> transferBetweenUserOwnedCards(
            @Parameter(description = "ID of the user performing the transfer", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody TransferRequest request) {
        transferService.transferBetweenUserOwnedCards(userId, request);
        return ResponseEntity.noContent().build();
    }
}