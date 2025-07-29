package com.example.bankcards.repository.specification;

import com.example.bankcards.dto.card.CardSearchRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CardSpecificationMapper {

    public Specification<Card> getCardSpecification(CardSearchRequest searchRequest) {
        List<Specification<Card>> specifications = new ArrayList<>();

        Optional.ofNullable(searchRequest.getStatus())
                .map(this::byStatus)
                .ifPresent(specifications::add);

        Optional.ofNullable(searchRequest.getExpiryDate())
                .map(this::byExpiryDate)
                .ifPresent(specifications::add);

        return specifications.stream()
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.isTrue(cb.literal(true)));
    }

    private Specification<Card> byExpiryDate(LocalDate date) {
        return (root, query, cb) -> cb.equal(root.get("expiryDate"), date);
    }

    private Specification<Card> byStatus(CardStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}