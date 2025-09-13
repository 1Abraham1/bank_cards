package com.abrik.bank_cards.bank_cards.security;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class MyUserDetails extends User {
    private final Long userId;
    private final List<SimpleGrantedAuthority> roles;

    public MyUserDetails(String username, String password, Long userId, List<SimpleGrantedAuthority> roles) {
        super(username, password, roles);
        this.userId = userId;
        this.roles = roles;
    }
}
