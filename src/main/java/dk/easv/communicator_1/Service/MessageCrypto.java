package dk.easv.communicator_1.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class MessageCrypto {
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Base64.Decoder MIME_DECODER = Base64.getDecoder();

    private MessageCrypto() {
    }

    public static String generateKey() {
        byte[] key = new byte[KEY_BYTES];
        RANDOM.nextBytes(key);
        return ENCODER.encodeToString(key);
    }

    public static String encrypt(String keyBase64, String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(keyBase64), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] packed = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
            return ENCODER.encodeToString(packed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message", e);
        }
    }

    public static String decrypt(String keyBase64, String packedBase64) {
        try {
            byte[] packed = decode(packedBase64);
            if (packed.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext too short");
            }

            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[packed.length - IV_BYTES];
            System.arraycopy(packed, 0, iv, 0, IV_BYTES);
            System.arraycopy(packed, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(keyBase64), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message", e);
        }
    }

    private static byte[] decode(String value) {
        String cleaned = value.replaceAll("\\s+", "");
        try {
            return URL_DECODER.decode(cleaned);
        } catch (IllegalArgumentException ignored) {
            return MIME_DECODER.decode(cleaned);
        }
    }

    private static SecretKeySpec secretKey(String keyBase64) {
        return new SecretKeySpec(decode(keyBase64), "AES");
    }
}
