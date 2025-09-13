package com.abrik.bank_cards.bank_cards.util;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class PanCryptoUtil {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public PanCryptoUtil(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String pan) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(pan.getBytes(StandardCharsets.UTF_8));

            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("PAN encryption failed", e);
        }
    }

    public String decrypt(String encrypted) throws Exception {
        byte[] allBytes = Base64.getDecoder().decode(encrypted);

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(allBytes, 0, iv, 0, IV_LENGTH);

        byte[] cipherText = new byte[allBytes.length - IV_LENGTH];
        System.arraycopy(allBytes, IV_LENGTH, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
