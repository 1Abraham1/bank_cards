package com.abrik.bank_cards.bank_cards.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRolesRequest {
    @NotEmpty(message = "Список ролей не должен быть пустым")
    private List<String> roles;
}
