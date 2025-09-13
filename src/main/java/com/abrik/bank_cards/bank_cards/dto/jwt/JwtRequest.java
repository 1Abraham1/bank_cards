package com.abrik.bank_cards.bank_cards.dto.jwt;

import lombok.Data;

@Data
public class JwtRequest {
    String username;
    String password;
}
