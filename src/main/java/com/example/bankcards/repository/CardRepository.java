package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {
    List<Card> findAllByOwner(UUID ownerId, Pageable pageable);

    @Query("SELECT SUM(c.balance) FROM Card c WHERE c.owner.id = :userId")
    BigDecimal sumBalanceByOwnerId(@Param("userId") UUID userId);
}