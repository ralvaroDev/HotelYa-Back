package pe.ralvaro.hotelyaback.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.ralvaro.hotelyaback.application.auth.SessionService;
import pe.ralvaro.hotelyaback.domain.OwnerSession;
import pe.ralvaro.hotelyaback.infrastructure.OwnerSessionRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionJwtValidatorTests {
    private final OwnerSessionRepository sessions = mock(OwnerSessionRepository.class);
    private final SessionJwtValidator validator = new SessionJwtValidator(new SessionService(sessions));

    private Jwt token(String sessionId, String subject) {
        var builder = Jwt.withTokenValue("test-token").header("alg", "HS256");
        if (sessionId != null) builder.claim("sid", sessionId);
        if (subject != null) builder.subject(subject);
        return builder.build();
    }

    @Test void acceptsAnActiveSessionOfTheTokenOwner() {
        var session = new OwnerSession(42L);
        when(sessions.findById(session.id)).thenReturn(Optional.of(session));
        assertFalse(validator.validate(token(session.id.toString(), "42")).hasErrors());
    }

    @Test void rejectsAbsentRevokedExpiredAndOtherOwnersSessions() {
        var session = new OwnerSession(42L);
        var jwt = token(session.id.toString(), "42");
        when(sessions.findById(session.id)).thenReturn(Optional.empty());
        assertTrue(validator.validate(jwt).hasErrors());
        when(sessions.findById(session.id)).thenReturn(Optional.of(session));
        session.revoked = true;
        assertTrue(validator.validate(jwt).hasErrors());
        session.revoked = false;
        session.expiresAt = Instant.now().minusSeconds(1);
        assertTrue(validator.validate(jwt).hasErrors());
        session.expiresAt = Instant.now().plusSeconds(60);
        assertTrue(validator.validate(token(session.id.toString(), "99")).hasErrors());
        assertTrue(validator.validate(token(session.id.toString(), null)).hasErrors());
    }

    @Test void rejectsMissingOrMalformedSessionIdsBeforeAccessingTheRepository() {
        assertTrue(validator.validate(token(null, "42")).hasErrors());
        assertTrue(validator.validate(token("malformed", "42")).hasErrors());
        verifyNoInteractions(sessions);
    }

    @Test void deniesAuthenticationWhenTheSessionStoreIsUnavailable() {
        var id = UUID.randomUUID();
        when(sessions.findById(id)).thenThrow(new IllegalStateException("Session store unavailable"));
        var result = validator.validate(token(id.toString(), "42"));
        assertTrue(result.hasErrors());
        assertEquals("invalid_token", result.getErrors().iterator().next().getErrorCode());
    }
}
