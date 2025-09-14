package com.abrik.bank_cards.bank_cards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ на запрос блокировки карты")
public class StatusResponse {

    @Schema(example = "7f6f2d4f-9f49-4c57-b0a9-f4a8a0b5a820")
    private UUID cardId;

    @Schema(example = "2025-09-12T01:45:00Z", description = "Когда запрос был установлен")
    private Instant requestedBlockAt;

    private CardStatus status;
}
