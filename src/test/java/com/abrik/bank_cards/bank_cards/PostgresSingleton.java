package com.abrik.bank_cards.bank_cards;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresSingleton {
    private static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                    .withDatabaseName("bank_cards")
                    .withUsername("bank_user")
                    .withPassword("bank_pass")
                    .withReuse(true); // опционально, см. примечание ниже

    static { INSTANCE.start(); }
    private PostgresSingleton() {}
    public static PostgreSQLContainer<?> getInstance() { return INSTANCE; }
}
