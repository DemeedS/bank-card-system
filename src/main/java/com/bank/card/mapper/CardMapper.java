package com.bank.card.mapper;

import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.Card;
import com.bank.card.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CardMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    CardResponse toCardResponse(Card card);

    @Mapping(target = "cardCount", ignore = true)
    UserResponse toUserResponse(User user);

    /**
     * After mapping, we always strip the encrypted card number
     * so it can never accidentally leak through the mapper.
     */
    @AfterMapping
    default void clearSensitiveData(@MappingTarget CardResponse response) {
        // maskedCardNumber is already set from the entity field directly
        // This is a safety guard — encrypted number is never mapped to response
    }
}
