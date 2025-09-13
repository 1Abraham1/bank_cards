package com.abrik.bank_cards.bank_cards.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Перевод между своими картами")
@Data
public class CreateTransferRequest {
    @NotNull @Schema(example = "5f1b6c3a-6b9c-4b7a-9e2a-68d9e1b0a1c2")
    private UUID fromCardId;

    @NotNull @Schema(example = "2a1c0e9b-4d3f-46a2-8f3a-0c9b8a7d6e5f")
    private UUID toCardId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true, message = "Сумма должна быть > 0")
    @Digits(integer = 17, fraction = 2)
    @Schema(example = "100.00")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency;

    @Size(max = 256)
    private String message;
}
