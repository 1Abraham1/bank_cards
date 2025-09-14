package com.abrik.bank_cards.bank_cards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/* Оставил поля для изменения,
* так как по ТЗ просили CRUD.
* в реальности вряд ли применяется */
@Data
public class UpdateCardRequest {
    @NotBlank
    @Pattern(regexp = "\\d{13,19}", message = "PAN должен содержать 13–19 цифр")
    @Schema(example = "5212345678901234")
    private String pan;

    @Min(1) @Max(12)
    @Schema(example = "9")
    private short expiryMonth;

    @Min(2024)
    @Schema(example = "2030")
    private short expiryYear;
}
