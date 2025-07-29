package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.card.CardNotOwnedByUserException;
import com.example.bankcards.exception.card.InvalidCardOperationException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UuidUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaTransferServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private JpaTransferService jpaTransferService;

    private UUID userId;
    private UUID fromCardId;
    private UUID toCardId;
    private User user;
    private Card fromCard;
    private Card toCard;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        fromCardId = UUID.randomUUID();
        toCardId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        fromCard = new Card(
                "encryptedFromCard",
                user,
                LocalDate.now().plusYears(1),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(200.00)
        );
        fromCard.setId(fromCardId);

        toCard = new Card(
                "encryptedToCard",
                user,
                LocalDate.now().plusYears(1),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(50.00)
        );
        toCard.setId(toCardId);

        transferRequest = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(100.00));

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(new UuidUserDetails(userId, "testuser", "password", Collections.emptyList()));
    }

    @Test
    void transferBetweenUserOwnedCards_validRequest_shouldPerformTransfer() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);

        assertEquals(BigDecimal.valueOf(100.00), fromCard.getBalance());
        assertEquals(BigDecimal.valueOf(150.00), toCard.getBalance());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(userRepository).getReferenceById(userId);
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transferBetweenUserOwnedCards_userNotFound_shouldThrowUserNotFoundException() {
        when(userRepository.existsById(userId)).thenReturn(false);

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("User not found with ID: " + userId + ".", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verifyNoInteractions(cardRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_fromCardNotFound_shouldThrowCardNotOwnedByUserException() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.empty());

        CardNotOwnedByUserException thrown = assertThrows(CardNotOwnedByUserException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Card with ID \"" + fromCardId + "\" isn't owned by user with ID \"" + userId + "\" or doesn't exist.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_toCardNotFound_shouldThrowCardNotOwnedByUserException() {
        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.empty());

        CardNotOwnedByUserException thrown = assertThrows(CardNotOwnedByUserException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Card with ID \"" + toCardId + "\" isn't owned by user with ID \"" + userId + "\" or doesn't exist.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_fromCardNotOwned_shouldThrowCardNotOwnedByUserException() {
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        fromCard.setOwner(differentUser);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        CardNotOwnedByUserException thrown = assertThrows(CardNotOwnedByUserException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Card with ID \"" + fromCardId + "\" isn't owned by user with ID \"" + userId + "\" or doesn't exist.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(userRepository).getReferenceById(userId);
        verifyNoMoreInteractions(cardRepository, userRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_toCardNotOwned_shouldThrowCardNotOwnedByUserException() {
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        toCard.setOwner(differentUser);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        CardNotOwnedByUserException thrown = assertThrows(CardNotOwnedByUserException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Card with ID \"" + toCardId + "\" isn't owned by user with ID \"" + userId + "\" or doesn't exist.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(userRepository).getReferenceById(userId);
        verifyNoMoreInteractions(cardRepository, userRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_fromCardNotActive_shouldThrowInvalidCardOperationException() {
        fromCard.setStatus(CardStatus.BLOCKED);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        InvalidCardOperationException thrown = assertThrows(InvalidCardOperationException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Source card is not active.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(userRepository).getReferenceById(userId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_toCardNotActive_shouldThrowInvalidCardOperationException() {
        toCard.setStatus(CardStatus.EXPIRED);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        InvalidCardOperationException thrown = assertThrows(InvalidCardOperationException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Destination card is not active.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(userRepository).getReferenceById(userId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_sameCard_shouldThrowInvalidCardOperationException() {
        transferRequest = new TransferRequest(fromCardId, fromCardId, BigDecimal.valueOf(100.00));

        when(userRepository.existsById(userId)).thenReturn(true);

        InvalidCardOperationException thrown = assertThrows(InvalidCardOperationException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Can't transfer money to the same card.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void transferBetweenUserOwnedCards_insufficientBalance_shouldThrowInvalidCardOperationException() {
        transferRequest = new TransferRequest(fromCardId, toCardId, BigDecimal.valueOf(300.00));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findById(fromCardId)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(toCardId)).thenReturn(Optional.of(toCard));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        InvalidCardOperationException thrown = assertThrows(InvalidCardOperationException.class, () -> {
            jpaTransferService.transferBetweenUserOwnedCards(userId, transferRequest);
        });

        assertEquals("Insufficient balance on source card.", thrown.getMessage());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findById(fromCardId);
        verify(cardRepository).findById(toCardId);
        verify(userRepository).getReferenceById(userId);
        verifyNoMoreInteractions(cardRepository);
    }
}