package pe.ralvaro.hotelyaback.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pe.ralvaro.hotelyaback.model.OwnerSession;

@Service
public class RefreshTokenService {
    private final SecureRandom random = new SecureRandom();

    public String rotate(OwnerSession session) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = session.id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.refreshHash = hash(token);
        return token;
    }

    public boolean matches(OwnerSession session, String token) {
        if (token == null || session.refreshHash == null) return false;
        return MessageDigest.isEqual(session.refreshHash.getBytes(StandardCharsets.US_ASCII),
            hash(token).getBytes(StandardCharsets.US_ASCII));
    }

    public Optional<UUID> sessionId(String token) {
        if (token == null || token.length() > 150) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(token.split("\\.", 2)[0]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
