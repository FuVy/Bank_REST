package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CardSearchRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Operations related to bank cards")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @Operation(summary = "Create a new card (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<CardDto> createCard(@Valid @RequestBody CreateCardRequest request) {
        CardDto createdCard = cardService.createCard(request);
        return new ResponseEntity<>(createdCard, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all cards with optional filtering and pagination (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CardDto>> getAllCards(
            @Parameter(description = "Page number (1-based index)", example = "1")
            @RequestParam(value = "page", required = false) Integer pageNumber,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "size", required = false) Integer pageSize,
            @Parameter(description = "Sort by creation date in ascending order (true for ASC, false for DESC)", example = "false")
            @RequestParam(value = "asc", defaultValue = "false") boolean ascendingCreationDate,
            @Parameter(description = "Search criteria for cards")
            @ModelAttribute CardSearchRequest searchRequest) {
        List<CardDto> cards = cardService.getAllCards(pageNumber, pageSize, ascendingCreationDate, searchRequest);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Get all cards for a specific user (Admin or User owning the cards)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards for the user",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role or user to be the owner",
                    content = @Content)
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CardDto>> getAllCardsForUser(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Page number (1-based index)", example = "1")
            @RequestParam(value = "page", required = false) Integer pageNumber,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "size", required = false) Integer pageSize,
            @Parameter(description = "Sort by creation date in ascending order (true for ASC, false for DESC)", example = "false")
            @RequestParam(value = "asc", defaultValue = "false") boolean ascendingCreationDate) {
        List<CardDto> cards = cardService.getAllCardsForUser(userId, pageNumber, pageSize, ascendingCreationDate);
        return ResponseEntity.ok(cards);
    }

    @Operation(summary = "Get card details by ID (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved card details",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content)
    })
    @GetMapping("/{cardId}")
    public ResponseEntity<CardDto> getCardById(
            @Parameter(description = "ID of the card to retrieve", required = true)
            @PathVariable UUID cardId) {
        CardDto card = cardService.getCardDtoById(cardId);
        return ResponseEntity.ok(card);
    }

    @Operation(summary = "Change the status of a card (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card status changed successfully",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Card not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Card status is already set to the requested status",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content)
    })
    @PatchMapping("/{cardId}/status")
    public ResponseEntity<Void> changeCardStatus(
            @Parameter(description = "ID of the card to update", required = true)
            @PathVariable UUID cardId,
            @Valid @RequestBody UpdateCardStatusRequest request) {
        cardService.changeCardStatus(cardId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "User blocks their own card (Admin or Card owner)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card blocked successfully",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User or Card not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Card is already blocked",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role or user to be the card owner",
                    content = @Content)
    })
    @PatchMapping("/{cardId}/block")
    public ResponseEntity<Void> userBlockCard(
            @Parameter(description = "ID of the card to block", required = true)
            @PathVariable UUID cardId) {
        cardService.userBlockCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a card (Admin only)", description = "Admin can delete any card by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can delete cards")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}