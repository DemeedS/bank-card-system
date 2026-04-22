package com.bank.card.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CardEncryptionService Tests")
class CardEncryptionServiceTest {

    private static final String VALID_KEY = "1234567890abcdef"; // exactly 16 chars

    private CardEncryptionService service;

    @BeforeEach
    void setUp() {
        service = new CardEncryptionService(VALID_KEY);
    }

    @Nested
    @DisplayName("Constructor key validation")
    class KeyValidationTests {

        @Test
        @DisplayName("Should throw when key is shorter than 16 bytes")
        void shouldThrowForShortKey() {
            assertThatThrownBy(() -> new CardEncryptionService("tooshort"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("16 UTF-8 bytes");
        }

        @Test
        @DisplayName("Should throw when key is longer than 16 bytes")
        void shouldThrowForLongKey() {
            assertThatThrownBy(() -> new CardEncryptionService("this-key-is-way-too-long"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("16 UTF-8 bytes");
        }

        @Test
        @DisplayName("Should construct successfully with exactly 16-byte key")
        void shouldConstructWithValidKey() {
            assertThatNoException().isThrownBy(() -> new CardEncryptionService(VALID_KEY));
        }
    }

    @Nested
    @DisplayName("Encrypt / Decrypt")
    class EncryptDecryptTests {

        @Test
        @DisplayName("Decrypting an encrypted value returns the original")
        void shouldRoundTrip() {
            String original = "1234567890123456";
            String encrypted = service.encrypt(original);
            assertThat(service.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("Encrypting the same value twice produces different ciphertexts (random IV)")
        void shouldProduceDifferentCiphertextsForSameInput() {
            String cardNumber = "1234567890123456";
            String first = service.encrypt(cardNumber);
            String second = service.encrypt(cardNumber);
            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("Tampered ciphertext fails to decrypt")
        void shouldFailOnTamperedCiphertext() {
            String encrypted = service.encrypt("1234567890123456");
            // Flip the last character to corrupt the GCM auth tag
            String tampered = encrypted.substring(0, encrypted.length() - 1) + "X";
            assertThatThrownBy(() -> service.decrypt(tampered))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to decrypt");
        }
    }

    @Nested
    @DisplayName("Mask")
    class MaskTests {

        @Test
        @DisplayName("Should mask all but the last four digits")
        void shouldMaskCorrectly() {
            assertThat(service.mask("1234567890123456")).isEqualTo("**** **** **** 3456");
        }

        @Test
        @DisplayName("Should strip spaces before masking")
        void shouldStripSpaces() {
            assertThat(service.mask("1234 5678 9012 3456")).isEqualTo("**** **** **** 3456");
        }

        @Test
        @DisplayName("Should throw when card number is too short")
        void shouldThrowForShortCardNumber() {
            assertThatThrownBy(() -> service.mask("123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("too short");
        }
    }
}
