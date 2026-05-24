package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    List<PaymentCard> findAllByUserId(Long userId);

    List<PaymentCard> findAllByUserIdAndActiveTrue(Long userId);

    boolean existsByNumber(String number);

    // Native SQL
    @Query(value = "SELECT COUNT(*) FROM payment_cards WHERE user_id = :userId",
            nativeQuery = true)
    int countCardsByUserId(@Param("userId") Long userId);
}