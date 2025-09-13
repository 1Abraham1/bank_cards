package com.abrik.bank_cards.bank_cards.security;

import com.abrik.bank_cards.bank_cards.entity.User;
import com.abrik.bank_cards.bank_cards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {
    private final UserService userService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(
                String.format("User '%s' not found", username)
        ));
        return new MyUserDetails(user.getUsername(), user.getPassword(), user.getId(),
                user.getRoles().stream().map(
                        role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList()));
    }
}
