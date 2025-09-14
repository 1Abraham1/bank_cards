package com.abrik.bank_cards.bank_cards.security;

import com.abrik.bank_cards.bank_cards.exception.AppError;
import com.abrik.bank_cards.bank_cards.exception.JwtAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        int code = HttpServletResponse.SC_UNAUTHORIZED;
        String msg = "Not authorized";

        if (ex instanceof DisabledException) {
            code = HttpServletResponse.SC_FORBIDDEN;   // 403 для заблокированных
            msg  = "User is disabled";
        } else if (ex instanceof BadCredentialsException) {
            code = HttpServletResponse.SC_UNAUTHORIZED;
            msg  = "Bad credentials";
        } else if (ex instanceof JwtAuthenticationException) {
            code = HttpServletResponse.SC_UNAUTHORIZED;
            msg  = ex.getMessage();
        }

        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        var body = new ObjectMapper().writeValueAsString(new AppError(code, msg));
        response.getWriter().write(body);
    }
}
