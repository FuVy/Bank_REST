package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CardSearchRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.card.CardNotFoundException;
import com.example.bankcards.exception.card.CardNotOwnedByUserException;
import com.example.bankcards.exception.card.CardStatusAlreadySetException;
import com.example.bankcards.exception.user.UserNotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.repository.specification.CardSpecificationMapper;
import com.example.bankcards.security.UuidUserDetails;
import com.example.bankcards.util.CardEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaCardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardEncryptionUtil cardEncryptionUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardMapper cardMapper;
    @Mock
    private CardSpecificationMapper cardSpecificationMapper;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private JpaCardService jpaCardService;

    private UUID cardId;
    private UUID ownerId;
    private User owner;
    private Card card;
    private CardDto cardDto;
    private CreateCardRequest createCardRequest;

    @BeforeEach
    void setUp() {
        cardId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        owner = new User();
        owner.setId(ownerId);
        owner.setUsername("testuser");

        card = new Card(
                "encryptedCardNumber",
                owner,
                LocalDate.now().plusYears(1),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(100.00)
        );
        card.setId(cardId);

        cardDto = new CardDto(
                cardId,
                "maskedCardNumber",
                ownerId,
                LocalDate.now().plusYears(1),
                CardStatus.ACTIVE,
                BigDecimal.valueOf(100.00)
        );

        createCardRequest = new CreateCardRequest(
                "1234567890123456",
                ownerId,
                LocalDate.now().plusYears(1),
                BigDecimal.valueOf(100.00)
        );

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createCard_validRequest_shouldReturnCardDto() {
        when(userRepository.existsById(ownerId)).thenReturn(true);
        when(userRepository.getReferenceById(ownerId)).thenReturn(owner);
        when(cardEncryptionUtil.encrypt(anyString())).thenReturn("encryptedCardNumber");
        when(cardRepository.save(any(Card.class))).thenReturn(card);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        CardDto result = jpaCardService.createCard(createCardRequest);

        assertNotNull(result);
        assertEquals(cardDto.getId(), result.getId());
        verify(userRepository).existsById(ownerId);
        verify(userRepository).getReferenceById(ownerId);
        verify(cardEncryptionUtil).encrypt("1234567890123456");
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toDto(card);
    }

    @Test
    void createCard_userNotFound_shouldThrowUserNotFoundException() {
        when(userRepository.existsById(ownerId)).thenReturn(false);

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaCardService.createCard(createCardRequest);
        });

        assertEquals("User not found with ID: " + ownerId + ".", thrown.getMessage());
        verify(userRepository).existsById(ownerId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(cardEncryptionUtil, cardRepository, cardMapper);
    }

    @Test
    void getAllCards_shouldReturnListOfCardDto() {
        Page<Card> cardPage = new PageImpl<>(List.of(card));
        when(cardSpecificationMapper.getCardSpecification(any(CardSearchRequest.class))).thenReturn(mock(Specification.class));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        List<CardDto> result = jpaCardService.getAllCards(1, 10, true, new CardSearchRequest(null, null));

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(cardDto.getId(), result.get(0).getId());
        verify(cardSpecificationMapper).getCardSpecification(any(CardSearchRequest.class));
        verify(cardRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(cardMapper).toDto(card);
    }

    @Test
    void getAllCardsForUser_validUser_shouldReturnListOfCardDto() {
        when(userRepository.existsById(ownerId)).thenReturn(true);
        when(cardRepository.findAllByOwner(eq(ownerId), any(Pageable.class))).thenReturn(List.of(card));
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        List<CardDto> result = jpaCardService.getAllCardsForUser(ownerId, 1, 10, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(cardDto.getId(), result.get(0).getId());
        verify(userRepository).existsById(ownerId);
        verify(cardRepository).findAllByOwner(eq(ownerId), any(Pageable.class));
        verify(cardMapper).toDto(card);
    }

    @Test
    void getAllCardsForUser_userNotFound_shouldThrowUserNotFoundException() {
        when(userRepository.existsById(ownerId)).thenReturn(false);

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () -> {
            jpaCardService.getAllCardsForUser(ownerId, 1, 10, true);
        });

        assertEquals("User not found with ID: " + ownerId + ".", thrown.getMessage());
        verify(userRepository).existsById(ownerId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(cardRepository, cardMapper);
    }

    @Test
    void getCardDtoById_existingCard_shouldReturnCardDto() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        CardDto result = jpaCardService.getCardDtoById(cardId);

        assertNotNull(result);
        assertEquals(cardDto.getId(), result.getId());
        verify(cardRepository).findById(cardId);
        verify(cardMapper).toDto(card);
    }

    @Test
    void getCardDtoById_nonExistingCard_shouldThrowCardNotFoundException() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        CardNotFoundException thrown = assertThrows(CardNotFoundException.class, () -> {
            jpaCardService.getCardDtoById(cardId);
        });

        assertEquals("Card not found with ID: " + cardId + ".", thrown.getMessage());
        verify(cardRepository).findById(cardId);
        verifyNoInteractions(cardMapper);
    }

    @Test
    void changeCardStatus_validRequest_shouldUpdateCardStatus() {
        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        jpaCardService.changeCardStatus(cardId, request);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(card);
    }

    @Test
    void changeCardStatus_cardNotFound_shouldThrowCardNotFoundException() {
        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        CardNotFoundException thrown = assertThrows(CardNotFoundException.class, () -> {
            jpaCardService.changeCardStatus(cardId, request);
        });

        assertEquals("Card not found with ID: " + cardId + ".", thrown.getMessage());
        verify(cardRepository).findById(cardId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void changeCardStatus_statusAlreadySet_shouldThrowCardStatusAlreadySetException() {
        UpdateCardStatusRequest request = new UpdateCardStatusRequest(CardStatus.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        CardStatusAlreadySetException thrown = assertThrows(CardStatusAlreadySetException.class, () -> {
            jpaCardService.changeCardStatus(cardId, request);
        });

        assertEquals("Card status already set for id: " + cardId + ".", thrown.getMessage());
        verify(cardRepository).findById(cardId);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void userBlockCard_validRequest_shouldBlockCard() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        UuidUserDetails userDetails = new UuidUserDetails(ownerId, "testuser", "password", Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(userRepository.getReferenceById(ownerId)).thenReturn(owner);
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        jpaCardService.userBlockCard(cardId);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).findById(cardId);
        verify(userRepository).getReferenceById(ownerId);
        verify(cardRepository).save(card);
    }

    @Test
    void userBlockCard_cardNotFound_shouldThrowCardNotOwnedByUserException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        UuidUserDetails userDetails = new UuidUserDetails(ownerId, "testuser", "password", Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        CardNotOwnedByUserException thrown = assertThrows(CardNotOwnedByUserException.class, () -> {
            jpaCardService.userBlockCard(cardId);
        });

        assertEquals("Card with ID \"" + cardId + "\" isn't owned by user with ID \"" + ownerId + "\" or doesn't exist.", thrown.getMessage());
        verify(cardRepository).findById(cardId);
        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(cardRepository);
    }

    @Test
    void userBlockCard_cardNotOwned_shouldThrowCardNotOwnedByUserException() {
        UUID differentOwnerId = UUID.randomUUID();
        User differentOwner = new User();
        differentOwner.setId(differentOwnerId);
        differentOwner.setUsername("differentuser");
        card.setOwner(differentOwner);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        UuidUserDetails userDetails = new UuidUserDetails(ownerId, "testuser", "password", Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(userRepository.getReferenceById(ownerId)).thenReturn(owner);

        CardNotOwnedByUserException thrown = assertThrows(CardNotOwnedByUserException.class, () -> {
            jpaCardService.userBlockCard(cardId);
        });

        assertEquals("Card with ID \"" + cardId + "\" isn't owned by user with ID \"" + ownerId + "\" or doesn't exist.", thrown.getMessage());
        verify(cardRepository).findById(cardId);
        verify(userRepository).getReferenceById(ownerId);
        verifyNoMoreInteractions(cardRepository, userRepository);
    }

    @Test
    void userBlockCard_statusAlreadyBlocked_shouldThrowCardStatusAlreadySetException() {
        card.setStatus(CardStatus.BLOCKED);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        UuidUserDetails userDetails = new UuidUserDetails(ownerId, "testuser", "password", Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(userRepository.getReferenceById(ownerId)).thenReturn(owner);

        CardStatusAlreadySetException thrown = assertThrows(CardStatusAlreadySetException.class, () -> {
            jpaCardService.userBlockCard(cardId);
        });

        assertEquals("Card status already set for id: " + cardId + ".", thrown.getMessage());
        verify(cardRepository).findById(cardId);
        verify(userRepository).getReferenceById(ownerId);
        verifyNoMoreInteractions(cardRepository, userRepository);
    }

    @Test
    void userBlockCard_noAuthentication_shouldThrowAccessDeniedException() {
        when(securityContext.getAuthentication()).thenReturn(null);

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            jpaCardService.userBlockCard(cardId);
        });

        assertEquals("Authentication required for this operation.", thrown.getMessage());
        verifyNoInteractions(cardRepository, userRepository, cardEncryptionUtil, cardMapper, cardSpecificationMapper);
    }

    @Test
    void userBlockCard_notAuthenticated_shouldThrowAccessDeniedException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            jpaCardService.userBlockCard(cardId);
        });

        assertEquals("Authentication required for this operation.", thrown.getMessage());
        verifyNoInteractions(cardRepository, userRepository, cardEncryptionUtil, cardMapper, cardSpecificationMapper);
    }

    @Test
    void userBlockCard_principalNotUuidUserDetails_shouldThrowAccessDeniedException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("someString");

        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () -> {
            jpaCardService.userBlockCard(cardId);
        });

        assertEquals("Authentication required for this operation.", thrown.getMessage());
        verifyNoInteractions(cardRepository, userRepository, cardEncryptionUtil, cardMapper, cardSpecificationMapper);
    }


    @Test
    void deleteCard_existingCard_shouldDeleteCard() {
        when(cardRepository.existsById(cardId)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(cardId);

        jpaCardService.deleteCard(cardId);

        verify(cardRepository).existsById(cardId);
        verify(cardRepository).deleteById(cardId);
    }

    @Test
    void deleteCard_nonExistingCard_shouldThrowCardNotFoundException() {
        when(cardRepository.existsById(cardId)).thenReturn(false);

        CardNotFoundException thrown = assertThrows(CardNotFoundException.class, () -> {
            jpaCardService.deleteCard(cardId);
        });

        assertEquals("Card not found with ID: " + cardId + ".", thrown.getMessage());
        verify(cardRepository).existsById(cardId);
        verifyNoMoreInteractions(cardRepository);
    }
}