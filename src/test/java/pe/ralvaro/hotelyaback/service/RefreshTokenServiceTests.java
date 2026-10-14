package pe.ralvaro.hotelyaback.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.ralvaro.hotelyaback.model.OwnerSession;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenServiceTests {
    private final RefreshTokenService service = new RefreshTokenService();

    @Test
    void rotationStoresOnlyTheHashAndIdentifiesTheSession() {
        var session = new OwnerSession(42L);
        var token = service.rotate(session);

        assertEquals(session.id, service.sessionId(token).orElseThrow());
        assertNotEquals(token, session.refreshHash);
        assertEquals(64, session.refreshHash.length());
        assertTrue(service.matches(session, token));
    }

    @Test
    void rotationInvalidatesThePreviousTokenWithoutExtendingTheSession() {
        var session = new OwnerSession(42L);
        var expiry = session.expiresAt;
        var previous = service.rotate(session);
        var current = service.rotate(session);

        assertNotEquals(previous, current);
        assertFalse(service.matches(session, previous));
        assertTrue(service.matches(session, current));
        assertEquals(expiry, session.expiresAt);
    }

    @Test
    void absentTokenOrHashDoesNotMatch() {
        var session = new OwnerSession(42L);
        assertFalse(service.matches(session, "anything"));
        service.rotate(session);
        assertFalse(service.matches(session, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "not-a-uuid.secret"})
    void malformedTokensHaveNoSession(String token) {
        assertTrue(service.sessionId(token).isEmpty());
    }

    @Test
    void oversizedTokensHaveNoSession() {
        assertTrue(service.sessionId("x".repeat(151)).isEmpty());
    }
}
