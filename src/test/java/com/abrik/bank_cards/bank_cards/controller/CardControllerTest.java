package com.abrik.bank_cards.bank_cards.controller;

import com.abrik.bank_cards.bank_cards.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CardController IT")
class CardControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("Create -> Get -> List (pagination/search/status) -> Update -> RequestBlock -> Delete")
    void fullCardFlow_success() throws Exception {
        // arrange user + jwt
        String username = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String fullName = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@test.com";
        String password = "Str0ng!Pass";
        String confirmPassword = "Str0ng!Pass";
        String jwt = registerAndGetJwt(username, fullName, email, password, confirmPassword);

        // create
        String pan = "5212345678901234";
        short month = 9;
        short year = 2030;
        String currency = "USD";
        BigDecimal balance = new BigDecimal("123.45");

        JsonNode created = createCard(jwt, pan, month, year, currency, balance);
        UUID cardId = UUID.fromString(created.path("id").asText());
        assertThat(cardId).isNotNull();

        // get by id → владелец должен видеть карту и panDecrypted
        mvc.perform(get("/api/cards/{cardId}", cardId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.maskedNumber").value(Matchers.matchesPattern("\\*{4} \\*{4} \\*{4} \\d{4}")))
                .andExpect(jsonPath("$.panDecrypted").value(pan))
                .andExpect(jsonPath("$.currency").value(currency))
                .andExpect(jsonPath("$.balance").value("123.45"));

        // list (page=0,size=10, sort=createdAt,desc), без фильтров
        mvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + jwt)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        // list: поиск по последним 4 цифрам (предположим, search работает по last4/пан/маске)
        String last4 = pan.substring(pan.length() - 4);
        mvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + jwt)
                        .param("search", last4)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(cardId.toString()));

        // update PAN/expiry
        String newPan = "5212345678909999";
        short newMonth = 10;
        short newYear = 2031;
        String updateBody = """
            { "pan":"%s", "expiryMonth":%d, "expiryYear":%d }
            """.formatted(newPan, newMonth, newYear);

        mvc.perform(patch("/api/cards/{cardId}", cardId)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId.toString()))
                .andExpect(jsonPath("$.panDecrypted").value(newPan))
                .andExpect(jsonPath("$.expiryMonth").value((int) newMonth))
                .andExpect(jsonPath("$.expiryYear").value((int) newYear));

        // request-block → статус и requestedBlockAt/flag
        String rbResp = mvc.perform(post("/api/cards/{cardId}/request-block", cardId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.requestedBlockAt").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode rbNode = om.readTree(rbResp);
        assertThat(UUID.fromString(rbNode.path("cardId").asText())).isEqualTo(cardId);
        assertThat(Instant.parse(rbNode.path("requestedBlockAt").asText())).isBeforeOrEqualTo(Instant.now());

        // delete
        mvc.perform(delete("/api/cards/{cardId}", cardId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // после delete — get должен вернуть 404 (или 403, если ты так реализуешь). Ожидаю 404:
        mvc.perform(get("/api/cards/{cardId}", cardId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Валидация: неверный PAN (меньше 13 цифр) → 400 Bad Request")
    void createCard_panValidationFails() throws Exception {
        // arrange user + jwt
        String username = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String fullName = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@test.com";
        String password = "Str0ng!Pass";
        String confirmPassword = "Str0ng!Pass";
        String jwt = registerAndGetJwt(username, fullName, email, password, confirmPassword);

        String invalidPan = "123456789012"; // 12 цифр — должно упасть по @Pattern \\d{13,19}
        String body = """
            {
              "pan":"%s",
              "expiryMonth":9,
              "expiryYear":2030,
              "currency":"USD",
              "balance":0
            }
            """.formatted(invalidPan);

        mvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Доступ без токена к защищённым эндпоинтам → 401")
    void securedEndpoints_requireJwt() throws Exception {
        mvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    { "pan":"5212345678901234","expiryMonth":9,"expiryYear":2030,"currency":"USD","balance":0 }
                """))
                .andExpect(status().isUnauthorized());
    }
}
