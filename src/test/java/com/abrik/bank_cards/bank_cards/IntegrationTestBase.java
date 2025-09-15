package com.abrik.bank_cards.bank_cards;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
@Rollback
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @Autowired
    MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    /* через Singleton гораздо быстрее */
    static final PostgreSQLContainer<?> POSTGRES = PostgresSingleton.getInstance();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.liquibase.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    /* @DirtiesContext Если хотим явно сбрасывать контекст между классами */
//    @Container
//    @ServiceConnection // главный твик — Spring сам подставит URL/логин/пароль
//    static final PostgreSQLContainer<?> POSTGRES =
//            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16")) // см. пункт 2
//                    .withDatabaseName("bank_cards")
//                    .withUsername("bank_user")
//                    .withPassword("bank_pass");

    protected String registerAndGetJwt(
            String username, String fullName, String email, String password, String confirmPassword) throws Exception {
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
                        .contentType(String.valueOf(org.junit.jupiter.api.extension.MediaType.APPLICATION_JSON))
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(String.valueOf(org.junit.jupiter.api.extension.MediaType.APPLICATION_JSON)))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.fullName").value(fullName))
                .andExpect(jsonPath("$.email").value(email));

        String authBody = """
            { "username":"%s", "password":"%s" }
            """.formatted(username, password);

        String authResponse = mvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        return om.readTree(authResponse).path("token").asText();
    }

    protected JsonNode createCard(String jwt,
                                String pan,
                                short month,
                                short year,
                                String currency,
                                BigDecimal balance) throws Exception {
        String body = """
            {
              "pan":"%s",
              "expiryMonth":%d,
              "expiryYear":%d,
              "currency":"%s",
              "balance":%s
            }
            """.formatted(pan, month, year, currency, balance == null ? "0" : balance.toPlainString());

        String resp = mvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.maskedNumber").value(Matchers.matchesPattern("\\*{4} \\*{4} \\*{4} \\d{4}")))
                .andExpect(jsonPath("$.expiryMonth").value((int) month))
                .andExpect(jsonPath("$.expiryYear").value((int) year))
                .andExpect(jsonPath("$.currency").value(currency))
                .andExpect(jsonPath("$.status").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        return om.readTree(resp);
    }

    protected UUID createCardAndGetId(String jwt, String pan, short month, short year, String currency, BigDecimal balance) throws Exception {
        String body = """
            {
              "pan":"%s",
              "expiryMonth":%d,
              "expiryYear":%d,
              "currency":"%s",
              "balance":%s
            }
            """.formatted(pan, (int) month, (int) year, currency, balance == null ? "0" : balance.toPlainString());

        String resp = mvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.maskedNumber").value(Matchers.matchesPattern("\\*{4} \\*{4} \\*{4} \\d{4}")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        return UUID.fromString(om.readTree(resp).path("id").asText());
    }
}
