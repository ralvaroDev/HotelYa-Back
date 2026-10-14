package pe.ralvaro.hotelyaback.presentation.auth.dto;

public record AuthResponse(String accessToken, long expiresIn, AuthUserResponse user) {}
