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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TransferController IT")
class TransferControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("POST /api/transfers — успешный перевод и идемпотентность по Idempotency-Key")
    void createTransfer_isIdempotent() throws Exception {
        // arrange: user + jwt + две карты
        String username = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String fullName = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@test.com";
        String password = "Str0ng!Pass";
        String confirmPassword = "Str0ng!Pass";
        String jwt = registerAndGetJwt(username, fullName, email, password, confirmPassword);

        UUID fromCard = createCardAndGetId(jwt, "5212345678901234", (short) 9, (short) 2030, "USD", new BigDecimal("500.00"));
        UUID toCard   = createCardAndGetId(jwt, "5212345678909999", (short)10, (short) 2031, "USD", new BigDecimal("10.00"));

        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String idemKey = UUID.randomUUID().toString();

        String req = """
            {
              "fromCardId":"%s",
              "toCardId":"%s",
              "amount":%s,
              "currency":"%s",
              "message":"test transfer"
            }
            """.formatted(fromCard, toCard, amount.toPlainString(), currency);

        // 1) первый запрос
        var res1 = mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + jwt)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andReturn();

        int status1 = res1.getResponse().getStatus();
        assertTrue(status1 == 201 || status1 == 200, "Expected 201 or 200 on first idempotent call");

        String body1 = res1.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode t1 = om.readTree(body1);
        UUID transferId1 = UUID.fromString(t1.path("id").asText());

        assertThat(transferId1).isNotNull();
        assertThat(UUID.fromString(t1.path("fromCardId").asText())).isEqualTo(fromCard);
        assertThat(UUID.fromString(t1.path("toCardId").asText())).isEqualTo(toCard);
        // amounts может сериализоваться как 100.0 — сравниваем через BigDecimal compareTo
        assertThat(new BigDecimal(t1.path("amounts").asText())).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.00"));
        assertThat(t1.path("currency").asText()).isEqualTo(currency);
        assertThat(t1.path("status").asText()).isNotBlank();

        // 2) повторный запрос с тем же Idempotency-Key — должен вернуть тот же перевод
        var res2 = mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + jwt)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andReturn();

        int status2 = res2.getResponse().getStatus();
        assertTrue(status2 == 201 || status2 == 200, "Expected 201 or 200 on second idempotent call");

        String body2 = res2.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode t2 = om.readTree(body2);
        UUID transferId2 = UUID.fromString(t2.path("id").asText());

        assertThat(transferId2).isEqualTo(transferId1);
    }

    @Test
    @DisplayName("GET /api/transfers — листинг своих переводов с фильтрами (status, from/to, cardId, пагинация)")
    void listOwn_withFilters() throws Exception {
        String username = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String fullName = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@test.com";
        String password = "Str0ng!Pass";
        String confirmPassword = "Str0ng!Pass";
        String jwt = registerAndGetJwt(username, fullName, email, password, confirmPassword);

        UUID fromCard = createCardAndGetId(jwt, "5212345678901234", (short) 9, (short) 2030, "USD", new BigDecimal("500.00"));
        UUID toCard   = createCardAndGetId(jwt, "5212345678909999", (short)10, (short) 2031, "USD", new BigDecimal("10.00"));

        String req = """
            {
              "fromCardId":"%s",
              "toCardId":"%s",
              "amount":%s,
              "currency":"USD",
              "message":"list test"
            }
            """.formatted(fromCard, toCard, new BigDecimal("50.00").toPlainString());

        // создаём один перевод
        mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + jwt)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isCreated());

        // фильтры
        Instant from = Instant.now().minusSeconds(3600);
        Instant to   = Instant.now().plusSeconds(3600);

        mvc.perform(get("/api/transfers")
                        .header("Authorization", "Bearer " + jwt)
                        .param("status", "COMPLETED")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("cardId", fromCard.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].fromCardId").value(fromCard.toString()))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }

    @Test
    @DisplayName("Валидация: amount <= 0 → 400 Bad Request")
    void createTransfer_amountValidation() throws Exception {
        String username = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String fullName = "u_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@test.com";
        String password = "Str0ng!Pass";
        String confirmPassword = "Str0ng!Pass";
        String jwt = registerAndGetJwt(username, fullName, email, password, confirmPassword);

        UUID fromCard = createCardAndGetId(jwt, "5212345678901234", (short) 9, (short) 2030, "USD", new BigDecimal("500.00"));
        UUID toCard   = createCardAndGetId(jwt, "5212345678909999", (short)10, (short) 2031, "USD", new BigDecimal("10.00"));

        String badReq = """
            {
              "fromCardId":"%s",
              "toCardId":"%s",
              "amount":0,
              "currency":"USD",
              "message":"zero"
            }
            """.formatted(fromCard, toCard);

        mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badReq))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        // при желании проверь поля error-ответа, если у тебя единый формат
    }

    @Test
    @DisplayName("Без токена доступ к /api/transfers запрещён → 401")
    void securedEndpoints_requireJwt() throws Exception {
        mvc.perform(get("/api/transfers"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "fromCardId":"5f1b6c3a-6b9c-4b7a-9e2a-68d9e1b0a1c2",
                      "toCardId":"2a1c0e9b-4d3f-46a2-8f3a-0c9b8a7d6e5f",
                      "amount":100.00,
                      "currency":"USD"
                    }
                """))
                .andExpect(status().isUnauthorized());
    }
}
