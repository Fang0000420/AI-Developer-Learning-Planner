package com.aidevplanner.backend.auth;

import com.aidevplanner.backend.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final long expirationSeconds;
    private final ObjectMapper objectMapper;
    private final byte[] secret;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:${JWT_SECRET:dev-only-change-me-secret}}") String secret,
            @Value("${app.jwt.expiration-seconds:${JWT_EXPIRATION_SECONDS:86400}}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("userId", user.getId());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

        String unsignedToken = base64Url(writeJson(header)) + "." + base64Url(writeJson(payload));
        return unsignedToken + "." + sign(unsignedToken);
    }

    public Optional<UserPrincipal> parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }
            Long userId = ((Number) payload.get("userId")).longValue();
            String username = String.valueOf(payload.get("sub"));
            return Optional.of(new UserPrincipal(userId, username));
        } catch (RuntimeException | IOException exception) {
            return Optional.empty();
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize JWT payload.", exception);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT.", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
