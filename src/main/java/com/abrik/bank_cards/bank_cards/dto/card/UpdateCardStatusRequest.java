package com.abrik.bank_cards.bank_cards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Смена статуса карты (ADMIN)")
public class UpdateCardStatusRequest {
    @NotNull
    private CardStatus status;
}
