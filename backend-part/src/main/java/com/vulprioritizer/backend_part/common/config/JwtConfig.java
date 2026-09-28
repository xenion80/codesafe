package com.vulprioritizer.backend_part.common.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    @Bean
    public SecretKey secretKey(){
        if (jwtSecretKey == null || jwtSecretKey.isBlank()) {
            jwtSecretKey = "dGhpc2lzYXZlcnlzZWN1cmVhbmRsb25namd0c2VjcmV0a2V5Zm9yY3liZXJ0b3RhbDEyMzQ1Njc4OTA=";
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        } catch (Exception e) {
            keyBytes = jwtSecretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
