package pe.ralvaro.hotelyaback.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.ralvaro.hotelyaback.dto.auth.AuthCredentials;
import pe.ralvaro.hotelyaback.exception.ApiFailure;
import pe.ralvaro.hotelyaback.model.OwnerAccount;
import pe.ralvaro.hotelyaback.model.OwnerSession;
import pe.ralvaro.hotelyaback.repository.OwnerAccountRepository;
import pe.ralvaro.hotelyaback.repository.OwnerSessionRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTests {
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AuthenticationManager authentication = mock(AuthenticationManager.class);
    private final JwtService jwt = mock(JwtService.class);
    private final OwnerAccountRepository accounts = mock(OwnerAccountRepository.class);
    private final OwnerSessionRepository sessions = mock(OwnerSessionRepository.class);
    private final RefreshTokenService refreshTokens = new RefreshTokenService();
    private final AuthService service = new AuthService(passwords, authentication, jwt, accounts, sessions, refreshTokens);
    private OwnerAccount account;

    @BeforeEach
    void setup() {
        account = new OwnerAccount("owner@example.com", "encoded-password");
        account.id = 42L;
    }

    @Test
    void registrationNormalizesEmailAndStoresAnEncodedPassword() {
        when(passwords.encode("password123")).thenReturn("encoded-password");
        when(accounts.saveAndFlush(any())).thenAnswer(call -> {
            OwnerAccount saved = call.getArgument(0);
            saved.id = 42L;
            return saved;
        });
        when(sessions.save(any())).thenAnswer(call -> call.getArgument(0));
        when(jwt.generateToken(eq(42L), any())).thenReturn("access-token");

        var result = service.register(new AuthCredentials(" Owner@Example.com ", "password123"));

        var saved = ArgumentCaptor.forClass(OwnerAccount.class);
        verify(accounts).saveAndFlush(saved.capture());
        assertEquals("owner@example.com", saved.getValue().email);
        assertEquals("encoded-password", saved.getValue().passwordHash);
        assertEquals("access-token", result.accessToken());
        assertEquals(42L, result.user().id());
        assertEquals(900, result.expiresIn());
        verify(sessions).save(any(OwnerSession.class));
    }

    @Test
    void duplicateRegistrationDoesNotCreateAnAccountOrSession() {
        when(accounts.findByEmail(account.email)).thenReturn(Optional.of(account));
        var failure = assertThrows(ApiFailure.class,
                () -> service.register(new AuthCredentials(account.email, "password123")));
        assertEquals(409, failure.status);
        assertTrue(failure.fields.containsKey("email"));
        verify(accounts, never()).saveAndFlush(any());
        verifyNoInteractions(sessions, jwt);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "a@b", "a b@example.com"})
    void invalidEmailsAreRejectedBeforePersistence(String email) {
        var failure = assertThrows(ApiFailure.class,
                () -> service.register(new AuthCredentials(email, "password123")));
        assertEquals(400, failure.status);
        assertTrue(failure.fields.containsKey("email"));
        verifyNoInteractions(accounts, sessions);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short"})
    void shortPasswordsAreRejectedBeforePersistence(String password) {
        assertPasswordRejected(password);
    }

    @Test
    void bcryptLimitUsesUtf8BytesRatherThanCharacterCount() {
        assertPasswordRejected("á".repeat(37));
    }

    private void assertPasswordRejected(String password) {
        var failure = assertThrows(ApiFailure.class,
                () -> service.register(new AuthCredentials(account.email, password)));
        assertEquals(400, failure.status);
        assertTrue(failure.fields.containsKey("password"));
        verifyNoInteractions(accounts, sessions, passwords);
    }

    @Test
    void validRefreshRotatesTheTokenAndPreservesAbsoluteExpiry() {
        var session = new OwnerSession(account.id);
        var previous = refreshTokens.rotate(session);
        var expiry = session.expiresAt;
        when(sessions.findByIdForUpdate(session.id)).thenReturn(Optional.of(session));
        when(accounts.findById(account.id)).thenReturn(Optional.of(account));
        when(jwt.generateToken(account.id, session.id)).thenReturn("new-access-token");

        var result = service.refresh(previous);

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertNotEquals(previous, result.refreshToken());
        assertTrue(refreshTokens.matches(session, result.refreshToken()));
        assertEquals(expiry, result.refreshExpiresAt());
        assertFalse(session.revoked);
    }

    @Test
    void replayRevokesTheSessionAndDoesNotIssueAnotherJwt() {
        var session = new OwnerSession(account.id);
        var stolen = refreshTokens.rotate(session);
        refreshTokens.rotate(session);
        when(sessions.findByIdForUpdate(session.id)).thenReturn(Optional.of(session));

        assertNull(service.refresh(stolen));
        assertTrue(session.revoked);
        verifyNoInteractions(jwt, accounts);
    }

    @Test
    void revokedAndExpiredSessionsCannotRefresh() {
        var session = new OwnerSession(account.id);
        var token = refreshTokens.rotate(session);
        when(sessions.findByIdForUpdate(session.id)).thenReturn(Optional.of(session));
        session.revoked = true;
        assertNull(service.refresh(token));
        session.revoked = false;
        session.expiresAt = Instant.now().minusSeconds(60);
        assertNull(service.refresh(token));
        verifyNoInteractions(jwt, accounts);
    }

    @Test
    void missingSessionCannotRefresh() {
        assertNull(service.refresh(null));
        assertNull(service.refresh("invalid"));
        assertNull(service.refresh(refreshTokens.rotate(new OwnerSession(42L))));
        verifyNoInteractions(jwt, accounts);
    }

    @Test
    void logoutRevokesOnlyTheMatchingSession() {
        var session = new OwnerSession(account.id);
        var token = refreshTokens.rotate(session);
        when(sessions.findByIdForUpdate(session.id)).thenReturn(Optional.of(session));
        service.logout(session.id + ".wrong");
        assertFalse(session.revoked);
        service.logout(token);
        assertTrue(session.revoked);
    }

    @Test
    void logoutWithoutACookieDoesNothing() {
        service.logout(null);
        verifyNoInteractions(sessions);
    }

    @Test
    void currentUserRequiresAnExistingAccount() {
        when(accounts.findById(account.id)).thenReturn(Optional.of(account));
        assertEquals(account.email, service.user(account.id).email());
        assertEquals(401, assertThrows(ApiFailure.class, () -> service.user(99L)).status);
    }
}
