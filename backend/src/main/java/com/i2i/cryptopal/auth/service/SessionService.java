package com.i2i.cryptopal.auth.service;

import com.i2i.cryptopal.common.exception.UnauthorizedException;
import com.i2i.cryptopal.user.entity.AppUser;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public SessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createSession(AppUser user) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
            SESSION_PREFIX + token,
            user.getId().toString(),
            SESSION_TTL
        );
        return token;
    }

    public Long requireUserId(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        String userIdValue = redisTemplate.opsForValue()
            .get(SESSION_PREFIX + token);

        if (userIdValue == null) {
            throw new UnauthorizedException("Session is invalid or has expired");
        }

        try {
            return Long.valueOf(userIdValue);
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("Session data is invalid");
        }
    }

    public void deleteSession(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        Boolean deleted = redisTemplate.delete(SESSION_PREFIX + token);

        if (!Boolean.TRUE.equals(deleted)) {
            throw new UnauthorizedException("Session is invalid or has expired");
        }
    }

    public long getExpirationSeconds() {
        return SESSION_TTL.toSeconds();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException(
                "Authorization header must contain a Bearer token"
            );
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            throw new UnauthorizedException("Session token cannot be empty");
        }

        return token;
    }
}