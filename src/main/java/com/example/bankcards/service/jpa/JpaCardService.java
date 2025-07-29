package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CardSearchRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.RoleConsts;
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
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.CardEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JpaCardService implements CardService {

    private final CardRepository cardRepository;
    private final CardEncryptionUtil cardEncryptionUtil;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;
    private final CardSpecificationMapper cardSpecificationMapper;

    private static final int MAX_PAGE_SIZE = 15;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int DEFAULT_PAGE = 0;

    @Override
    @Transactional
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    public CardDto createCard(CreateCardRequest request) {
        if (!userRepository.existsById(request.getOwnerId())) {
            throw new UserNotFoundException(request.getOwnerId());
        }

        User proxyOwner = userRepository.getReferenceById(request.getOwnerId());

        Card card = new Card(
                cardEncryptionUtil.encrypt(request.getCardNumber()),
                proxyOwner,
                request.getExpiryDate(),
                CardStatus.ACTIVE,
                request.getInitialBalance()
        );
        Card savedCard = cardRepository.save(card);
        return cardMapper.toDto(savedCard);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    public List<CardDto> getAllCards(Integer pageNumber, Integer pageSize, boolean ascendingCreationDate, CardSearchRequest searchRequest) {
        Pageable pageable = buildPageRequest(pageNumber, pageSize, getCreationDateOrder(ascendingCreationDate));
        Specification<Card> cardSpecification = cardSpecificationMapper.getCardSpecification(searchRequest);
        return cardRepository.findAll(cardSpecification, pageable)
                .map(cardMapper::toDto)
                .getContent();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "') or " +
            "(#userId.equals(authentication.principal.uuid))")
    public List<CardDto> getAllCardsForUser(UUID userId, Integer pageNumber, Integer pageSize, boolean ascendingCreationDate) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        Pageable pageable = buildPageRequest(pageNumber, pageSize, getCreationDateOrder(ascendingCreationDate));
        return cardRepository.findAllByOwner(userId, pageable).stream().map(cardMapper::toDto).toList();
    }

    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    @Transactional(readOnly = true)
    public CardDto getCardDtoById(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        return cardMapper.toDto(card);
    }

    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    @Transactional
    public void changeCardStatus(UUID cardId, UpdateCardStatusRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        if (card.getStatus() == request.getNewStatus()) {
            throw new CardStatusAlreadySetException(cardId);
        }
        card.setStatus(request.getNewStatus());
        cardRepository.save(card);
    }

    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "') or " +
            "(#userId.equals(authentication.principal.uuid))")
    @Transactional
    public void userBlockCard(UUID cardId) {
        UUID userId = getUserIdFromAuthenticationContext();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotOwnedByUserException(cardId, userId));

        checkCardsForOwnership(userId, card);

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardStatusAlreadySetException(cardId);
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Override
    @PreAuthorize("hasRole('" + RoleConsts.ADMIN + "')")
    public void deleteCard(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException(cardId);
        }
        cardRepository.deleteById(cardId);
    }

    private Sort.Order getCreationDateOrder(boolean asc) {
        return new Sort.Order(asc ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt");
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize, Sort.Order... orders) {
        int queryPageNumber;
        int queryPageSize;

        if (pageNumber != null && pageNumber > 0) {
            queryPageNumber = pageNumber - 1;
        } else {
            queryPageNumber = DEFAULT_PAGE;
        }

        if (pageSize == null || pageSize < 1) {
            queryPageSize = DEFAULT_PAGE_SIZE;
        } else {
            queryPageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        }
        Sort sort = Sort.by(orders);

        return PageRequest.of(queryPageNumber, queryPageSize, sort);
    }

    private UUID getUserIdFromAuthenticationContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required for this operation.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UuidUserDetails userDetails) {
            return userDetails.getUuid();
        } else {
            throw new AccessDeniedException("Authentication required for this operation.");
        }
    }

    private void checkCardsForOwnership(UUID userToCheck, Card... cards) {
        User userProxy = userRepository.getReferenceById(userToCheck);
        for (Card card : cards) {
            if (!card.getOwner().equals(userProxy)) {
                throw new CardNotOwnedByUserException(card.getId(), userToCheck);
            }
        }
    }
}