package com.example.musicrecommendation.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AESGcmTextEncryptor {

    private static final int IV_LEN = 12;   // 96-bit
    private static final int TAG_LEN = 128; // bits

    private final String keyB64;
    private SecretKey secretKey;
    private final SecureRandom rnd = new SecureRandom();

    public AESGcmTextEncryptor(@Value("${app.crypto.chat.aesKeyBase64}") String keyB64) {
        this.keyB64 = keyB64;
    }

    @PostConstruct
    void init() {
        try {
            if (keyB64 != null && !keyB64.trim().isEmpty()) {
                byte[] raw = Base64.getDecoder().decode(keyB64);
                if (raw.length != 32) {
                    System.out.println("Warning: AES key length incorrect, disabling encryption");
                    this.secretKey = null;
                } else {
                    this.secretKey = new SecretKeySpec(raw, "AES");
                }
            } else {
                System.out.println("Warning: No AES key provided, disabling encryption");
                this.secretKey = null;
            }
        } catch (Exception e) {
            System.out.println("Warning: AES key initialization failed, disabling encryption: " + e.getMessage());
            this.secretKey = null;
        }
    }

    /** returns [ cipherTextBase64, ivBase64 ] */
    public String[] encrypt(String plaintext) {
        if (secretKey == null) {
            // 암호화 비활성화됨 - 플레인텍스트를 Base64로만 인코딩
            String encoded = Base64.getEncoder().encodeToString(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new String[]{encoded, "no-iv"};
        }
        
        try {
            byte[] iv = new byte[IV_LEN];
            rnd.nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LEN, iv));
            byte[] cipher = c.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return new String[]{
                    Base64.getEncoder().encodeToString(cipher),
                    Base64.getEncoder().encodeToString(iv)
            };
        } catch (Exception e) {
            throw new RuntimeException("encrypt error", e);
        }
    }

    public String decrypt(String cipherB64, String ivB64) {
        if (secretKey == null || "no-iv".equals(ivB64)) {
            // 암호화 비활성화됨 - Base64 디코딩만 수행
            try {
                byte[] decoded = Base64.getDecoder().decode(cipherB64);
                return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "[decode_error]";
            }
        }
        
        try {
            byte[] cipher = Base64.getDecoder().decode(cipherB64);
            byte[] iv = Base64.getDecoder().decode(ivB64);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LEN, iv));
            byte[] plain = c.doFinal(cipher);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "[decrypt_error]";
        }
    }
}
