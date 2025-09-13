package com.abrik.bank_cards.bank_cards.security;

import com.abrik.bank_cards.bank_cards.exception.AppError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        var body = new ObjectMapper()
                .writeValueAsString(new AppError(HttpServletResponse.SC_FORBIDDEN, "Access is denied"));

        response.getWriter().write(body);
    }
}
