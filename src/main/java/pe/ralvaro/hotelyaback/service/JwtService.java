package pe.ralvaro.hotelyaback.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final Duration ACCESS_TOKEN_TTL=Duration.ofMinutes(15);
    private final JwtEncoder encoder;

    public JwtService(JwtEncoder encoder) { this.encoder=encoder; }

    public String generateToken(Long ownerId, UUID sessionId) {
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("hotelya").subject(ownerId.toString())
            .audience(List.of("hotelya-owner")).issuedAt(now).expiresAt(now.plus(ACCESS_TOKEN_TTL))
            .claim("sid",sessionId.toString()).build();
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
}
