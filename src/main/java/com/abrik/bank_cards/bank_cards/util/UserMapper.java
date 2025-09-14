package com.abrik.bank_cards.bank_cards.util;

import com.abrik.bank_cards.bank_cards.dto.user.UserDto;
import com.abrik.bank_cards.bank_cards.entity.Role;
import com.abrik.bank_cards.bank_cards.entity.User;

import java.util.List;

public final class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .active(u.getActive())
                .roles(u.getRoles() == null ? List.of() :
                        u.getRoles().stream().map(Role::getName).toList())
                .build();
    }
}
