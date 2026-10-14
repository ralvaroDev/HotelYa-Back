package pe.ralvaro.hotelyaback.security;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import pe.ralvaro.hotelyaback.owner.ApiFailure;
@Service
@Transactional
public class AuthService {
    @PersistenceContext private EntityManager entities;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final SecureRandom random=new SecureRandom();
    public AuthService(PasswordEncoder passwords, JwtEncoder encoder) { this.passwords=passwords; this.encoder=encoder; }
    public record Credentials(String email,String password) {}
    public record User(Long id,String email) {}
    public record Tokens(String accessToken, long expiresIn, User user, String refreshToken, Instant refreshExpiresAt) {}
    public String normalizedEmail(String email) {
        if (email==null || email.trim().length()>254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw ApiFailure.field("email","Introduce un correo válido.");
        return email.trim().toLowerCase(Locale.ROOT);
    }
    private Optional<OwnerAccount> byEmail(String email) { return entities.createQuery("from OwnerAccount where email=:email",OwnerAccount.class).setParameter("email",email).getResultStream().findFirst(); }
    public Tokens register(Credentials request) {
        String email=normalizedEmail(request.email());
        validatePassword(request.password());
        if (byEmail(email).isPresent()) throw new ApiFailure(409,"Ya existe una cuenta con ese correo.",Map.of("email","Este correo ya está registrado."));
        var account=new OwnerAccount(email,passwords.encode(request.password())); entities.persist(account); entities.flush();
        return newSession(account);
    }
    public void validatePassword(String password) {
        if (password==null || password.length()<8 || password.getBytes(StandardCharsets.UTF_8).length>72) throw ApiFailure.field("password","Usa al menos 8 caracteres y como máximo 72 bytes.");
    }
    public Tokens login(Credentials request) {
        String email=normalizedEmail(request.email());
        var account=byEmail(email).orElse(null);
        if (account==null || request.password()==null || !passwords.matches(request.password(),account.passwordHash)) throw new ApiFailure(401,"Correo o contraseña incorrectos.");
        return newSession(account);
    }
    private Tokens newSession(OwnerAccount account) {
        var session=new OwnerSession(account.id); String token=rotate(session); entities.persist(session); return tokens(account,session,token);
    }
    private String rotate(OwnerSession session) {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=session.id+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); session.refreshHash=hash(token); return token;
    }
    private Tokens tokens(OwnerAccount account, OwnerSession session,String refresh) {
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("hotelya").subject(account.id.toString()).audience(List.of("hotelya-owner"))
            .issuedAt(now).expiresAt(now.plusSeconds(900)).claim("sid",session.id.toString()).build();
        String access=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Tokens(access,900,new User(account.id,account.email),refresh,session.expiresAt);
    }
    private OwnerSession cookieSession(String token) {
        if (token==null || token.length()>150) return null;
        try { return entities.find(OwnerSession.class,UUID.fromString(token.split("\\.",2)[0]),LockModeType.PESSIMISTIC_WRITE); }
        catch (IllegalArgumentException e) { return null; }
    }
    public Tokens refresh(String token) {
        var session=cookieSession(token);
        if (session==null || session.revoked || !session.expiresAt.isAfter(Instant.now())) return null;
        if (!MessageDigest.isEqual(session.refreshHash.getBytes(StandardCharsets.US_ASCII),hash(token).getBytes(StandardCharsets.US_ASCII))) {
            session.revoked=true; return null;
        }
        return tokens(entities.find(OwnerAccount.class,session.ownerId),session,rotate(session));
    }
    public void logout(String token) {
        var session=cookieSession(token);
        if (session!=null && MessageDigest.isEqual(session.refreshHash.getBytes(StandardCharsets.US_ASCII),hash(token).getBytes(StandardCharsets.US_ASCII))) session.revoked=true;
    }
    @Transactional(readOnly=true) public boolean active(Jwt jwt) {
        try {
            var s=entities.find(OwnerSession.class,UUID.fromString(jwt.getClaimAsString("sid")));
            return s!=null && !s.revoked && s.expiresAt.isAfter(Instant.now()) && s.ownerId.toString().equals(jwt.getSubject());
        } catch (RuntimeException e) { return false; }
    }
    @Transactional(readOnly=true) public User user(Long id) {
        var a=entities.find(OwnerAccount.class,id); if (a==null) throw new ApiFailure(401,"Inicia sesión para continuar."); return new User(a.id,a.email);
    }
    public OwnerAccount bootstrap(String email,String password) {
        email=normalizedEmail(email); validatePassword(password);
        final String normalized=email;
        return byEmail(email).orElseGet(() -> { var account=new OwnerAccount(normalized,passwords.encode(password)); entities.persist(account); entities.flush(); return account; });
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
