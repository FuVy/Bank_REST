package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.util.CardEncryptionUtil;
import com.example.bankcards.util.CardMaskingUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring", uses = {
        CardEncryptionUtil.class
})
public abstract class CardMapper {

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected CardEncryptionUtil cardEncryptionUtil;

    @Mapping(target = "maskedCardNumber", source = "encryptedCardNumber", qualifiedByName = "maskCardNumber")
    @Mapping(target = "ownerId", source = "owner", qualifiedByName = "getOwnerId")
    public abstract CardDto toDto(Card card);

    @Named("maskCardNumber")
    protected String maskCardNumber(String encryptedCardNumber) {
        String decryptedCardNumber = cardEncryptionUtil.decrypt(encryptedCardNumber);
        return CardMaskingUtil.maskCardNumber(decryptedCardNumber);
    }

    @Named("getOwnerId")
    protected UUID getOwnerId(User owner) {
        return owner != null ? owner.getId() : null;
    }
}
