package com.example.bankcards.controller;

import com.example.bankcards.dto.user.EditUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing users (Admin only)")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get all users (paginated and sortable)", description = "Retrieve a paginated list of all users, sortable by creation date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users."),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can access.")
    })
    @GetMapping
    public ResponseEntity<List<UserDto>> findAllUsers(
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(defaultValue = "true") boolean ascending) {
        return ResponseEntity.ok(userService.findAllUsers(pageNumber, pageSize, ascending));
    }

    @Operation(summary = "Get user by username", description = "Retrieve a specific user by their username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can access.")
    })
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        UserDto userDto = userService.findByUsername(username);
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "Update user data", description = "Update user's username, roles, and manage owned cards (append/remove).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "409", description = "Username already taken."),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can update other users.")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateUserData(@PathVariable UUID id, @Valid @RequestBody EditUserRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a user", description = "Delete a user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can delete users.")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}