package com.abrik.bank_cards.bank_cards;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStartupIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextLoadsAndLiquibaseApplied() {
        // Проверим, что Liquibase прогнал changeSet'ы
        Integer changelogCount = jdbc.queryForObject(
                "select count(*) from databasechangelog",
                Integer.class
        );
        assertThat(changelogCount)
                .as("Liquibase changesets should be applied")
                .isNotNull()
                .isGreaterThan(0);

        // Проверим, что есть ключевая таблица из миграций (подставь свою)
        Integer usersCount = jdbc.queryForObject(
                "select count(*) from users",
                Integer.class
        );
        assertThat(usersCount)
                .as("Table 'users' should exist after migrations")
                .isNotNull();
    }
}
