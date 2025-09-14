package com.abrik.bank_cards.bank_cards.dto.user;

import lombok.Data;

@Data
public class RegistrationUserDto {
    private String username;
    private String password;
    private String fullName;
    private String confirmPassword;
    private String email;
}
