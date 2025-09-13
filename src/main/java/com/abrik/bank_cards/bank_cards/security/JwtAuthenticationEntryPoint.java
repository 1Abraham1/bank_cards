package com.abrik.bank_cards.bank_cards.security;

import com.abrik.bank_cards.bank_cards.exception.AppError;
import com.abrik.bank_cards.bank_cards.exception.JwtAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String msg = (ex instanceof JwtAuthenticationException)
                ? ex.getMessage()
                : "Not authorized";

        var body = new ObjectMapper()
                .writeValueAsString(new AppError(HttpServletResponse.SC_UNAUTHORIZED, msg));

        response.getWriter().write(body);
    }
}
