package pe.ralvaro.hotelyaback.dto.auth;

import java.time.Instant;

public record AuthTokens(String accessToken, long expiresIn, AuthUser user, String refreshToken, Instant refreshExpiresAt) {}
