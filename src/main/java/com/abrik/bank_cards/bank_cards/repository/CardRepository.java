package com.abrik.bank_cards.bank_cards.repository;

import com.abrik.bank_cards.bank_cards.dto.card.CardStatus;
import com.abrik.bank_cards.bank_cards.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    Page<Card> findAllByUserId(Long userId, Pageable pageable);

    Page<Card> findAllByUserIdAndStatus(Long userId, CardStatus status, Pageable pageable);

    Optional<Card> findByIdAndUserId(UUID id, Long userId);

    void deleteByIdAndUserId(UUID cardId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id and c.userId = :userId")
    Optional<Card> lockByIdAndUserId(@Param("id") UUID id, @Param("userId") Long userId);

    // универсальный поиск для админа и пользователя
    @Query("""
    select c from Card c
    where (:userId is null or c.userId = :userId)
      and (:status is null or c.status = :status)
      and (
            (:qPattern is null and :last4Pattern is null)
         or (:qPattern is not null and lower(c.currency) like :qPattern)
         or (:last4Pattern is not null and cast(c.last4 as string) like :last4Pattern)
      )
""")
    Page<Card> search(
            @Param("userId") Long userId,
            @Param("status") CardStatus status,
            @Param("qPattern") String qPattern,
            @Param("last4Pattern") String last4Pattern,
            Pageable pageable
    );
}
