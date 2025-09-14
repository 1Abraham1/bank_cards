package com.abrik.bank_cards.bank_cards.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CreateUserResponse {
    private Long id;
    private String username;
    private String email;
}
