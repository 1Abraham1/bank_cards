package com.abrik.bank_cards.bank_cards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Смена статуса карты (ADMIN)")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCardStatusRequest {
    @NotNull
    private CardStatus status;
}