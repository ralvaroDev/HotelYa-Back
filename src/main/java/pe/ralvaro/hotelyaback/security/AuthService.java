package pe.ralvaro.hotelyaback.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.owner.ApiFailure;
import pe.ralvaro.hotelyaback.infrastructure.OwnerAccountRepository;
import pe.ralvaro.hotelyaback.infrastructure.OwnerSessionRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class AuthService {
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final OwnerAccountRepository accounts;
    private final OwnerSessionRepository sessions;

    public AuthService(PasswordEncoder passwords, AuthenticationManager authenticationManager, JwtService jwtService, OwnerAccountRepository accounts, OwnerSessionRepository sessions, RefreshTokenService refreshTokens) {
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accounts = accounts;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
    }

    public String normalizedEmail(String email) {
        if (email == null || email.trim().length() > 254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw ApiFailure.field("email", "Introduce un correo válido.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Optional<OwnerAccount> byEmail(String email) {
        return accounts.findByEmail(email);
    }

    public Tokens register(Credentials request) {
        String email = normalizedEmail(request.email());
        validatePassword(request.password());
        if (byEmail(email).isPresent())
            throw new ApiFailure(409, "Ya existe una cuenta con ese correo.", Map.of("email", "Este correo ya está registrado."));
        var account = new OwnerAccount(email, passwords.encode(request.password()));
        account = accounts.saveAndFlush(account);
        return newSession(account);
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw ApiFailure.field("password", "Usa al menos 8 caracteres y como máximo 72 bytes.");
    }

    public Tokens login(Credentials request) {
        String email = normalizedEmail(request.email());
        final OwnerPrincipal principal;
        try {
            var authenticated = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
            principal = (OwnerPrincipal) authenticated.getPrincipal();
        } catch (AuthenticationException e) {
            throw new ApiFailure(401, "Correo o contraseña incorrectos.");
        }
        return newSession(accounts.findById(principal.ownerId()).orElseThrow(() -> new ApiFailure(401, "Inicia sesión para continuar.")));
    }

    private Tokens newSession(OwnerAccount account) {
        var session = new OwnerSession(account.id);
        String token = refreshTokens.rotate(session);
        session = sessions.save(session);
        return tokens(account, session, token);
    }

    private Tokens tokens(OwnerAccount account, OwnerSession session, String refresh) {
        String access = jwtService.generateToken(account.id, session.id);
        return new Tokens(access, JwtService.ACCESS_TOKEN_TTL.toSeconds(), new User(account.id, account.email), refresh, session.expiresAt);
    }

    private OwnerSession cookieSession(String token) {
        return refreshTokens.sessionId(token).flatMap(sessions::findByIdForUpdate).orElse(null);
    }

    public Tokens refresh(String token) {
        var session = cookieSession(token);
        if (session == null || session.revoked || !session.expiresAt.isAfter(Instant.now())) return null;
        if (!refreshTokens.matches(session, token)) {
            session.revoked = true;
            return null;
        }
        return tokens(accounts.findById(session.ownerId).orElseThrow(() -> new ApiFailure(401, "Inicia sesión para continuar.")), session, refreshTokens.rotate(session));
    }

    public void logout(String token) {
        var session = cookieSession(token);
        if (session != null && refreshTokens.matches(session, token))
            session.revoked = true;
    }

    @Transactional(readOnly = true)
    public boolean active(Jwt jwt) {
        try {
            var s = sessions.findById(UUID.fromString(jwt.getClaimAsString("sid"))).orElse(null);
            return s != null && !s.revoked && s.expiresAt.isAfter(Instant.now()) && s.ownerId.toString().equals(jwt.getSubject());
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public User user(Long id) {
        var a = accounts.findById(id).orElse(null);
        if (a == null) throw new ApiFailure(401, "Inicia sesión para continuar.");
        return new User(a.id, a.email);
    }

    public OwnerAccount bootstrap(String email, String password) {
        email = normalizedEmail(email);
        validatePassword(password);
        final String normalized = email;
        return byEmail(email).orElseGet(() -> {
            var account = new OwnerAccount(normalized, passwords.encode(password));
            account = accounts.saveAndFlush(account);
            return account;
        });
    }

    public record Credentials(String email, String password) {
    }

    public record User(Long id, String email) {
    }

    public record Tokens(String accessToken, long expiresIn, User user, String refreshToken, Instant refreshExpiresAt) {
    }
}
