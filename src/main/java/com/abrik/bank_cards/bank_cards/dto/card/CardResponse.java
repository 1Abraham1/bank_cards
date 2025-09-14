package com.abrik.bank_cards.bank_cards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Карта")
public class CardResponse {
    @Schema(example = "7f6f2d4f-9f49-4c57-b0a9-f4a8a0b5a820")
    private UUID id;

    private Long userId;

    @Schema(example = "**** **** **** 1234")
    private String maskedNumber;

    private String panDecrypted;

    @Schema(example = "9")
    private short expiryMonth;

    @Schema(example = "2030")
    private short expiryYear;

    private CardStatus status;

    @Schema(example = "0.00")
    private BigDecimal balance;

    @Schema(example = "USD")
    private String currency;

    @Schema(description = "Пользователь запросил блокировку")
    private boolean requestedBlock;

    private Instant createdAt;
    private Instant updatedAt;
}
