package pe.ralvaro.hotelyaback.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import pe.ralvaro.hotelyaback.dto.auth.AuthCredentials;
import pe.ralvaro.hotelyaback.dto.auth.AuthTokens;
import pe.ralvaro.hotelyaback.dto.auth.AuthUser;
import pe.ralvaro.hotelyaback.dto.request.AuthCredentialsRequest;
import pe.ralvaro.hotelyaback.dto.response.AuthResponse;
import pe.ralvaro.hotelyaback.dto.response.AuthUserResponse;
import pe.ralvaro.hotelyaback.exception.ApiFailure;
import pe.ralvaro.hotelyaback.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean secure;

    public AuthController(AuthService auth, @Value("${owner.cookie.secure:false}") boolean secure) {
        this.auth = auth;
        this.secure = secure;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrf, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("token", csrf.getToken(), "headerName", csrf.getHeaderName());
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthCredentialsRequest body, HttpServletResponse response) {
        response.setStatus(201);
        return send(auth.register(new AuthCredentials(body.email(), body.password())), response);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthCredentialsRequest body, HttpServletResponse response) {
        return send(auth.login(new AuthCredentials(body.email(), body.password())), response);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@CookieValue(name = "HOTELYA_REFRESH", required = false) String cookie,
            HttpServletResponse response) {
        var tokens = auth.refresh(cookie);
        if (tokens == null) {
            clear(response);
            throw new ApiFailure(401, "Tu sesión ha terminado. Vuelve a ingresar.");
        }
        return send(tokens, response);
    }

    @PostMapping("/logout")
    public void logout(@CookieValue(name = "HOTELYA_REFRESH", required = false) String cookie,
            HttpServletResponse response) {
        auth.logout(cookie);
        clear(response);
        response.setStatus(204);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return userResponse(auth.user(Long.valueOf(jwt.getSubject())));
    }

    private AuthResponse send(AuthTokens tokens, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.addHeader("Set-Cookie", cookie(tokens.refreshToken())
            .maxAge(Duration.between(Instant.now(), tokens.refreshExpiresAt())).build().toString());
        return new AuthResponse(tokens.accessToken(), tokens.expiresIn(), userResponse(tokens.user()));
    }

    private AuthUserResponse userResponse(AuthUser user) {
        return new AuthUserResponse(user.id(), user.email());
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from("HOTELYA_REFRESH", value).httpOnly(true).secure(secure)
            .sameSite(secure ? "None" : "Lax").path("/api/auth");
    }

    private void clear(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.addHeader("Set-Cookie", cookie("").maxAge(0).build().toString());
    }
}
