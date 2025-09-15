package com.abrik.bank_cards.bank_cards.controller;

import com.abrik.bank_cards.bank_cards.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper om = new ObjectMapper();

    private final String username = "u_" + UUID.randomUUID().toString().substring(0, 8);
    private final String fullName = "Testyy";
    private final String email = username + "@test.com";
    private final String password = "StrongPass!123";
    private final String confirmPassword = "StrongPass!123";

    @BeforeEach
    public void setup() throws Exception {
        // корректная регистрация
        String registerBody = """
            {
              "username": "%s",
              "fullName": "%s",
              "email": "%s",
              "password": "%s",
              "confirmPassword": "%s"
            }
            """.formatted(username, fullName, email, password, confirmPassword);

        mvc.perform(post("/api/register")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(String.valueOf(MediaType.APPLICATION_JSON)))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.fullName").value(fullName))
                .andExpect(jsonPath("$.email").value(email));

    }

    @Test
    @DisplayName("Register → Auth → Delete account (happy path)")
    void register_auth_delete_success() throws Exception {

        // 2) попытка авторизации с верным паролем
        String authBody = """
            { "username": "%s", "password": "%s" }
            """.formatted(username, password);

        String authResponse = mvc.perform(post("/api/auth")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(authBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(String.valueOf(MediaType.APPLICATION_JSON)))
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode tokenNode = om.readTree(authResponse);
        String jwt = tokenNode.path("token").asText();
        assertThat(jwt).isNotBlank();

        // DELETE /api/account с токеном
        mvc.perform(delete("/api/account")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auth с неверным паролем → 401 Unauthorized")
    void auth_wrong_password_unauthorized() throws Exception {
        // попытка авторизации с неверным паролем
        String badAuthBody = """
            { "username": "%s", "password": "%s" }
            """.formatted(username, "WRONG_" + password);

        mvc.perform(post("/api/auth")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(badAuthBody))
                .andExpect(status().isUnauthorized());
    }
}
