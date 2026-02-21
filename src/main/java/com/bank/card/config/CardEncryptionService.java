package com.bank.card.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES-128 encryption for card numbers.
 * The raw card number is NEVER stored in plain text.
 */
@Component
@Slf4j
public class CardEncryptionService {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec secretKeySpec;

    public CardEncryptionService(@Value("${card.encryption.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes();
        // AES-128 requires exactly 16 bytes
        byte[] key = new byte[16];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 16));
        this.secretKeySpec = new SecretKeySpec(key, ALGORITHM);
    }

    public String encrypt(String cardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt card number", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedCardNumber);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt card number", e);
        }
    }

    /**
     * Produces masked display: **** **** **** 1234
     */
    public String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Card number too short to mask");
        }
        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
