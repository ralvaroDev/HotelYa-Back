package pe.ralvaro.hotelyaback.security;

import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pe.ralvaro.hotelyaback.service.SessionService;

@Component
public class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final SessionService sessions;

    public SessionJwtValidator(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            if (sessions.isSessionActive(UUID.fromString(jwt.getClaimAsString("sid")), jwt.getSubject())) {
                return OAuth2TokenValidatorResult.success();
            }
        } catch (RuntimeException ignored) {
            // Missing/malformed claims or an unavailable session store must deny authentication.
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sesión no válida.", null));
    }
}
