package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.user.BalanceDto;
import com.example.bankcards.dto.user.EditUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.user.UserAlreadyExistsException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private JpaUserService jpaUserService;

    private UUID userId;
    private String username;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        username = "testuser";
        user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setCreatedAt(LocalDateTime.now());
        Role userRole = new Role();
        userRole.setName(RoleName.USER);
        user.setRoles(Collections.singleton(userRole));


        userDto = new UserDto(userId, username, List.of("USER"), LocalDateTime.now());

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void findByUsername_existingUser_shouldReturnUserDto() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = jpaUserService.findByUsername(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userRepository).findByUsername(username);
        verify(userMapper).toDto(user);
    }

    @Test
    void findByUsername_nonExistingUser_shouldThrowUserNotFoundException() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaUserService.findByUsername(username);
        });

        assertEquals("User not found with username: " + username + ".", thrown.getMessage());
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findById_existingUser_shouldReturnUserDto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = jpaUserService.findById(userId);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    void findById_nonExistingUser_shouldThrowUserNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaUserService.findById(userId);
        });

        assertEquals("User not found with ID: " + userId + ".", thrown.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    void findAllUsers_shouldReturnListOfUserDto() {
        Page<User> userPage = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(userDto);

        List<UserDto> result = jpaUserService.findAllUsers(1, 10, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(userDto.getUsername(), result.get(0).getUsername());
        verify(userRepository).findAll(any(Pageable.class));
        verify(userMapper).toDto(user);
    }

    @Test
    void deleteUser_existingUser_shouldDeleteUser() {
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);

        jpaUserService.deleteUser(userId);

        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_nonExistingUser_shouldThrowUserNotFoundException() {
        when(userRepository.existsById(userId)).thenReturn(false);

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaUserService.deleteUser(userId);
        });

        assertEquals("User not found with ID: " + userId + ".", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateUser_existingUserAndNewUsername_shouldUpdateUsername() {
        String newUsername = "newtestuser";
        EditUserRequest request = new EditUserRequest(newUsername);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(newUsername)).thenReturn(false);

        jpaUserService.updateUser(userId, request);

        assertEquals(newUsername, user.getUsername());
        verify(userRepository).findById(userId);
        verify(userRepository).existsByUsername(newUsername);
    }

    @Test
    void updateUser_existingUserAndSameUsername_shouldNotUpdateUsername() {
        EditUserRequest request = new EditUserRequest(username);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        jpaUserService.updateUser(userId, request);

        assertEquals(username, user.getUsername());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void updateUser_existingUserAndNullUsername_shouldNotUpdateUsername() {
        EditUserRequest request = new EditUserRequest(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        jpaUserService.updateUser(userId, request);

        assertEquals(username, user.getUsername());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void updateUser_userNotFound_shouldThrowUserNotFoundException() {
        EditUserRequest request = new EditUserRequest("newUsername");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaUserService.updateUser(userId, request);
        });

        assertEquals("User not found with ID: " + userId + ".", thrown.getMessage());
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void updateUser_usernameAlreadyExists_shouldThrowUserAlreadyExistsException() {
        String existingUsername = "existingUser";
        EditUserRequest request = new EditUserRequest(existingUsername);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(existingUsername)).thenReturn(true);

        UserAlreadyExistsException thrown = assertThrows(UserAlreadyExistsException.class, () -> {
            jpaUserService.updateUser(userId, request);
        });

        assertEquals("User with username \"" + existingUsername + "\" already exists.", thrown.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository).existsByUsername(existingUsername);
    }

    @Test
    void getBalanceForUser_existingUser_shouldReturnBalanceDto() {
        BigDecimal totalBalance = BigDecimal.valueOf(250.75);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.sumBalanceByOwnerId(userId)).thenReturn(totalBalance);

        BalanceDto result = jpaUserService.getBalanceForUser(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(totalBalance, result.getTotalBalance());
        verify(userRepository).existsById(userId);
        verify(cardRepository).sumBalanceByOwnerId(userId);
    }

    @Test
    void getBalanceForUser_userNotFound_shouldThrowUserNotFoundException() {
        when(userRepository.existsById(userId)).thenReturn(false);

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaUserService.getBalanceForUser(userId);
        });

        assertEquals("User not found with ID: " + userId + ".", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verifyNoInteractions(cardRepository);
    }
}