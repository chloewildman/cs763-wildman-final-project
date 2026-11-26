package edu.bu.met.wildmancs763.discussions.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class FieldEncryptionService {

    private static final String VERSION_PREFIX = "v1:";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public FieldEncryptionService(@Value("${app.encryption.field-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY must be configured.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY must be a Base64-encoded AES-256 key.", exception);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY must decode to exactly 32 bytes.");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce)
                    .put(encrypted)
                    .array();
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt the moderation note.", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(VERSION_PREFIX)) {
            throw new IllegalStateException("Moderation note ciphertext has an unsupported format.");
        }
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(ciphertext.substring(VERSION_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Moderation note ciphertext is invalid.", exception);
        }
        if (payload.length <= NONCE_LENGTH) {
            throw new IllegalStateException("Moderation note ciphertext is truncated.");
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        byte[] encrypted = new byte[payload.length - NONCE_LENGTH];
        System.arraycopy(payload, 0, nonce, 0, NONCE_LENGTH);
        System.arraycopy(payload, NONCE_LENGTH, encrypted, 0, encrypted.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Moderation note authentication failed.", exception);
        }
    }
}
