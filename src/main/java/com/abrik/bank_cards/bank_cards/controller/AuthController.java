package com.abrik.bank_cards.bank_cards.controller;

import com.abrik.bank_cards.bank_cards.dto.jwt.JwtRequest;
import com.abrik.bank_cards.bank_cards.dto.jwt.JwtResponse;
import com.abrik.bank_cards.bank_cards.dto.RegistrationUserDto;
import com.abrik.bank_cards.bank_cards.dto.UserDto;
import com.abrik.bank_cards.bank_cards.security.MyUserDetails;
import com.abrik.bank_cards.bank_cards.service.AuthService;
import com.abrik.bank_cards.bank_cards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/auth")
    public ResponseEntity<JwtResponse> createAuthToken(@RequestBody JwtRequest authRequest) {
        return ResponseEntity.ok(authService.createAuthToken(authRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> createNewUser(@RequestBody RegistrationUserDto registrationUserDto) {
        return ResponseEntity.ok(userService.createNewUser(registrationUserDto));
    }

    @DeleteMapping("/account")
    void deleteAccount(@RequestBody MyUserDetails myUserDetails) {
        userService.deleteAccount(myUserDetails.getUserId());
    }
}
