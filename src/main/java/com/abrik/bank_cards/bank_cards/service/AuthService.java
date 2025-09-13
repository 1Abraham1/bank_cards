package com.abrik.bank_cards.bank_cards.service;

import com.abrik.bank_cards.bank_cards.security.JwtUserDetailsService;
import com.abrik.bank_cards.bank_cards.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.abrik.bank_cards.bank_cards.dto.jwt.JwtRequest;
import com.abrik.bank_cards.bank_cards.dto.jwt.JwtResponse;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUserDetailsService jwtUserDetailsService;
    private final JwtTokenUtil jwtTokenUtils;
    private final AuthenticationManager authenticationManager;

    public JwtResponse createAuthToken(JwtRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(authRequest.getUsername());
        String token = jwtTokenUtils.generateToken(userDetails);
        return new JwtResponse(token);
    }
}
