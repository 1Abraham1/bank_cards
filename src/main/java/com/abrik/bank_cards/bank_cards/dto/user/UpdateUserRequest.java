package com.abrik.bank_cards.bank_cards.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRequest {
    @Email(message = "Некорректный email")
    private String email;

    @Size(min = 1, max = 255, message = "Имя должно быть от 1 до 255 символов")
    private String fullName;

    private Boolean active;
}
