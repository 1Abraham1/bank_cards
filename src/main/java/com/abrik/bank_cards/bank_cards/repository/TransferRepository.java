package com.abrik.bank_cards.bank_cards.repository;

import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import com.abrik.bank_cards.bank_cards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Optional<Transfer> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("""
    select t from Transfer t
    where t.userId = :userId
      and t.status = coalesce(:status, t.status)
      and t.createdAt >= coalesce(:fromTs, t.createdAt)
      and t.createdAt <  coalesce(:toTs,   t.createdAt)
      and (
           t.fromCardId = coalesce(:cardId, t.fromCardId)
        or t.toCardId   = coalesce(:cardId, t.toCardId)
      )
    order by t.createdAt desc
    """)
    Page<Transfer> searchUser(
            @Param("userId") Long userId,
            @Param("status") TransferStatus status,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("cardId") UUID cardId,
            Pageable pageable
    );

    @Query("""
    select t from Transfer t
    where t.status = coalesce(:status, t.status)
      and t.createdAt >= coalesce(:fromTs, t.createdAt)
      and t.createdAt <  coalesce(:toTs,   t.createdAt)
      and (
           t.fromCardId = coalesce(:cardId, t.fromCardId)
        or t.toCardId   = coalesce(:cardId, t.toCardId)
      )
    order by t.createdAt desc
    """)
    Page<Transfer> searchAdmin(
            @Param("status") TransferStatus status,
            @Param("fromTs") Instant from,
            @Param("toTs") Instant to,
            @Param("cardId") UUID cardId,
            Pageable pageable
    );
}
