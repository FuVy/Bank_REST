package com.example.bankcards.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class CardEncryptionUtil {

    @Value("${app.encryption.secret}")
    private String secretKey;

    @Value("${app.encryption.salt}")
    private String salt;

    private BytesEncryptor bytesEncryptor;

    @PostConstruct
    public void init() {
        this.bytesEncryptor = Encryptors.stronger(secretKey, salt);
    }

    public String encrypt(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = bytesEncryptor.encrypt(bytes);

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    public String decrypt(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        byte[] encryptedBytes = Base64.getDecoder().decode(text);
        byte[] decryptedBytes = bytesEncryptor.decrypt(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
