package com.innowise.userservice.repository.specification;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecification {

    public static Specification<PaymentCard> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<PaymentCard> hasHolder(String holder) {
        return (root, query, cb) -> {
            if (holder == null || holder.isBlank()) return cb.conjunction();;
            return cb.like(cb.lower(root.get("holder")), "%" + holder.toLowerCase() + "%");
        };
    }
}
