package com.abrik.bank_cards.bank_cards.entity;

import com.abrik.bank_cards.bank_cards.dto.card.CardStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "card")
public class Card {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "last4", length = 4, nullable = false)
    private String last4;

    // опционально, можно удалить
    @Column(name = "pan_encrypted")
    private String panEncrypted;

    @Column(name = "expiry_month")
    private short expiryMonth;

    @Column(name = "expiry_year")
    private short expiryYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status; // ACTIVE, BLOCKED, EXPIRED

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "requested_block_at")
    private Instant requestedBlockAt; // null, если нет запроса

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
