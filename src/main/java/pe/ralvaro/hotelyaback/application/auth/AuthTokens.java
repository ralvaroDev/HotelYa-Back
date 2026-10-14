package pe.ralvaro.hotelyaback.application.auth;

import java.time.Instant;

public record AuthTokens(String accessToken, long expiresIn, AuthUser user, String refreshToken, Instant refreshExpiresAt) {}
