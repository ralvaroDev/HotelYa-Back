package pe.ralvaro.hotelyaback.dto.response;

public record AuthResponse(String accessToken, long expiresIn, AuthUserResponse user) {}
