package com.example.bankcards.controller;

import com.example.bankcards.dto.user.EditUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.exception.user.UserAlreadyExistsException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.security.JwtAuthenticationEntryPoint;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.security.JwtUserDetailsService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtUserDetailsService jwtUserDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private String username;
    private UserDto userDto;
    private EditUserRequest editUserRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        username = "testuser";
        userDto = new UserDto(userId, username, List.of("USER"), LocalDateTime.now());
        editUserRequest = new EditUserRequest("newusername");
    }

    @Test
    void findAllUsers_shouldReturnOkAndListOfUsers() throws Exception {
        when(userService.findAllUsers(any(), any(), anyBoolean())).thenReturn(List.of(userDto));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].username").value(username));
    }

    @Test
    void getUserByUsername_existingUser_shouldReturnOkAndUserDto() throws Exception {
        when(userService.findByUsername(eq(username))).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/username/{username}", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void getUserByUsername_nonExistingUser_shouldReturnNotFound() throws Exception {
        when(userService.findByUsername(eq(username)))
                .thenThrow(new UserNotFoundException(username));

        mockMvc.perform(get("/api/v1/users/username/{username}", username))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_existingUser_shouldReturnOkAndUserDto() throws Exception {
        when(userService.findById(eq(userId))).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void getUserById_nonExistingUser_shouldReturnNotFound() throws Exception {
        when(userService.findById(eq(userId)))
                .thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserData_validRequest_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).updateUser(eq(userId), any(EditUserRequest.class));

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editUserRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateUserData_userNotFound_shouldReturnNotFound() throws Exception {
        doThrow(new UserNotFoundException(userId)).when(userService)
                .updateUser(eq(userId), any(EditUserRequest.class));

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editUserRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserData_usernameAlreadyExists_shouldReturnConflict() throws Exception {
        doThrow(new UserAlreadyExistsException("existingUser"))
                .when(userService).updateUser(eq(userId), any(EditUserRequest.class));

        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editUserRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUserData_invalidUsernameLength_shouldReturnBadRequest() throws Exception {
        EditUserRequest invalidRequest = new EditUserRequest("a");
        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        invalidRequest = new EditUserRequest("a".repeat(100));
        mockMvc.perform(patch("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_existingUser_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(eq(userId));

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_nonExistingUser_shouldReturnNotFound() throws Exception {
        doThrow(new UserNotFoundException(userId)).when(userService).deleteUser(eq(userId));

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound());
    }
}