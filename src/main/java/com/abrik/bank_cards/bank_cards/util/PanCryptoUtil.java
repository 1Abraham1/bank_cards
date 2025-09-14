package com.abrik.bank_cards.bank_cards.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Set;

@Component
public class PanCryptoUtil {
    @Value("${app.security.pan-key}")
    private String secretBase64;

    private SecretKeySpec keySpec;
    private static final String ALGORITHM = "AES";

    /**
     * Разрешённые длины AES-ключа (в байтах).
     */
    private static final Set<Integer> ALLOWED_KEY_LENGTHS = Set.of(16, 24, 32);

    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(secretBase64);
        int keyLen = decodedKey.length;

        if (!ALLOWED_KEY_LENGTHS.contains(keyLen)) {
            throw new IllegalArgumentException(
                    "Invalid AES key length: " + keyLen
                            + " bytes (allowed: " + ALLOWED_KEY_LENGTHS + ")"
            );
        }

        keySpec = new SecretKeySpec(decodedKey, ALGORITHM);
    }

    public String encrypt(String pan) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(pan.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encryptedPan) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedPan);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
