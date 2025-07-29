package com.example.bankcards.controller;

import com.example.bankcards.dto.security.JwtAuthResponse;
import com.example.bankcards.dto.security.LoginRequest;
import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.exception.InternalException;
import com.example.bankcards.exception.user.UserAlreadyExistsException;
import com.example.bankcards.security.JwtAuthenticationEntryPoint;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.security.JwtUserDetailsService;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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

    @Test
    void login_validCredentials_shouldReturnOkAndJwtAuthResponse() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        JwtAuthResponse jwtAuthResponse = new JwtAuthResponse("mockedToken");

        when(authService.login(any(LoginRequest.class))).thenReturn(jwtAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("mockedToken")));
    }

    @Test
    void login_invalidCredentials_shouldReturnBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("wronguser", "wrongpassword");

        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankUsername_shouldReturnBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_blankPassword_shouldReturnBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_newUser_shouldReturnCreatedAndJwtAuthResponse() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("newuser", "newpassword");
        JwtAuthResponse jwtAuthResponse = new JwtAuthResponse("newMockedToken");

        when(authService.register(any(RegisterRequest.class))).thenReturn(jwtAuthResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("newMockedToken")));
    }

    @Test
    void register_existingUser_shouldReturnConflict() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("existinguser", "password");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("existinguser"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_internalException_shouldReturnInternalServerError() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("newuser", "newpassword");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new InternalException("Role 'USER' not found"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void register_blankUsername_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("", "password");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_usernameTooShort_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("ab", "password");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_usernameTooLong_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("a".repeat(51), "password");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("testuser", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordTooShort_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("testuser", "abc");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_passwordTooLong_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("testuser", "a".repeat(101));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
}