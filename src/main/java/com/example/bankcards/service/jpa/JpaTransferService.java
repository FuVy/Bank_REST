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
import com.example.bankcards.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JpaTransferService implements TransferService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    @Override
    @PreAuthorize("(#userId.equals(authentication.principal.uuid))")
    @Transactional
    public void transferBetweenUserOwnedCards(UUID userId, TransferRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new CardNotOwnedByUserException(request.getFromCardId(), userId));
        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new CardNotOwnedByUserException(request.getFromCardId(), userId));

        checkCardsForOwnership(userId, fromCard, toCard);

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Source card is not active.");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidCardOperationException("Destination card is not active.");
        }
        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidCardOperationException("Can't transfer money to the same card.");
        }
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InvalidCardOperationException("Insufficient balance on source card.");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
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
