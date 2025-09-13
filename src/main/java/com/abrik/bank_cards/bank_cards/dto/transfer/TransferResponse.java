package com.abrik.bank_cards.bank_cards.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Информация о переводе")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
    private UUID id;

    private Long userId;

    private UUID fromCardId;

    private UUID toCardId;

    private BigDecimal amounts;

    @Schema(example = "USD")
    private String currency;

    private TransferStatus status;

    @Schema(description = "Причина ошибки (если есть)")
    private String message;

    private Instant createdAt;
}
