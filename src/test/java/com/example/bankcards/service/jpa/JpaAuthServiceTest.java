package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.security.JwtAuthResponse;
import com.example.bankcards.dto.security.LoginRequest;
import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InternalException;
import com.example.bankcards.exception.user.UserAlreadyExistsException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private JpaAuthService authService;

    @Test
    void login_validCredentials_shouldReturnJwtAuthResponse() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(jwtTokenProvider.generateToken("testuser")).thenReturn("mockedJwtToken");
        SecurityContextHolder.setContext(securityContext);

        JwtAuthResponse result = authService.login(loginRequest);

        assertThat(result.getToken()).isEqualTo("mockedJwtToken");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext, times(1)).setAuthentication(authentication);
        verify(jwtTokenProvider, times(1)).generateToken("testuser");
    }

    @Test
    void login_invalidCredentials_shouldThrowBadCredentialsException() {
        LoginRequest loginRequest = new LoginRequest("wronguser", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateToken(anyString());
    }

    @Test
    void register_newUser_shouldRegisterAndReturnJwtAuthResponse() {
        RegisterRequest registerRequest = new RegisterRequest("newuser", "newpassword");
        Role userRole = new Role();
        userRole.setName(RoleName.USER);
        User savedUser = new User("newuser", "encodedpassword");
        savedUser.setRoles(Set.of(userRole));
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedpassword");
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getName()).thenReturn("newuser");
        when(jwtTokenProvider.generateToken("newuser")).thenReturn("mockedJwtToken");
        SecurityContextHolder.setContext(securityContext);

        JwtAuthResponse result = authService.register(registerRequest);

        assertThat(result.getToken()).isEqualTo("mockedJwtToken");
        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(passwordEncoder, times(1)).encode("newpassword");
        verify(roleRepository, times(1)).findByName(RoleName.USER);
        verify(userRepository, times(1)).save(any(User.class));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext, times(1)).setAuthentication(authentication);
        verify(jwtTokenProvider, times(1)).generateToken("newuser");
    }

    @Test
    void register_existingUser_shouldThrowUserAlreadyExistsException() {
        RegisterRequest registerRequest = new RegisterRequest("existinguser", "password");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));

        verify(userRepository, times(1)).existsByUsername("existinguser");
        verify(passwordEncoder, never()).encode(anyString());
        verify(roleRepository, never()).findByName(any(RoleName.class));
        verify(userRepository, never()).save(any(User.class));
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void register_roleNotFound_shouldThrowInternalException() {
        RegisterRequest registerRequest = new RegisterRequest("newuser", "newpassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodedpassword");
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        assertThrows(InternalException.class, () -> authService.register(registerRequest));

        verify(userRepository, times(1)).existsByUsername("newuser");
        verify(passwordEncoder, times(1)).encode("newpassword");
        verify(roleRepository, times(1)).findByName(RoleName.USER);
        verify(userRepository, never()).save(any(User.class));
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}