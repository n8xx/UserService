package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {
    long countByUserId(Long userId);
}