package com.abrik.bank_cards.bank_cards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCardRequest {
    @NotNull
    Long userId;

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

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Код валюты ISO-4217")
    @Schema(example = "USD")
    private String currency;

    private BigDecimal balance;
}