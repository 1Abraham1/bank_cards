package com.abrik.bank_cards.bank_cards.dto.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Boolean active;
    private List<String> roles;
}