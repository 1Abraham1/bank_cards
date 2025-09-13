package com.abrik.bank_cards.bank_cards.config;

import com.abrik.bank_cards.bank_cards.util.PanCryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Bean
    public PanCryptoUtil panCryptoUtil(
            @Value("${app.security.pan-key}") String base64Key) {
        return new PanCryptoUtil(base64Key);
    }
}