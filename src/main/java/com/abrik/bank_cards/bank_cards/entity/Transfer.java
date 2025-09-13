package com.abrik.bank_cards.bank_cards.entity;

import com.abrik.bank_cards.bank_cards.dto.transfer.TransferStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "transfer")
public class Transfer {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "from_card_id", nullable = false)
    private UUID fromCardId;

    @Column(name = "to_card_id", nullable = false)
    private UUID toCardId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TransferStatus status;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "idempotency_key", length = 64, nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
