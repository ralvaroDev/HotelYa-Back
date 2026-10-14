package pe.ralvaro.hotelyaback.application.auth;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.infrastructure.OwnerSessionRepository;

@Service
@Transactional(readOnly = true)
public class SessionService {
    private final OwnerSessionRepository sessions;

    public SessionService(OwnerSessionRepository sessions) { this.sessions = sessions; }

    public boolean isSessionActive(UUID sessionId, String ownerSubject) {
        return sessions.findById(sessionId)
            .filter(session -> !session.revoked && session.expiresAt.isAfter(Instant.now())
                && session.ownerId.toString().equals(ownerSubject))
            .isPresent();
    }
}
