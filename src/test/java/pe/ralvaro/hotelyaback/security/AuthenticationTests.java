package pe.ralvaro.hotelyaback.security;

import pe.ralvaro.hotelyaback.application.auth.AuthService;
import pe.ralvaro.hotelyaback.application.auth.AuthCredentials;
import pe.ralvaro.hotelyaback.application.auth.AuthUser;
import pe.ralvaro.hotelyaback.application.auth.JwtService;
import pe.ralvaro.hotelyaback.application.auth.RefreshTokenService;
import pe.ralvaro.hotelyaback.domain.OwnerAccount;
import pe.ralvaro.hotelyaback.domain.OwnerSession;

import pe.ralvaro.hotelyaback.infrastructure.OwnerAccountRepository;
import pe.ralvaro.hotelyaback.infrastructure.OwnerSessionRepository;
import java.util.Optional;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import pe.ralvaro.hotelyaback.owner.ApiFailure;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationTests {
    private static final String SECRET="hotelya-test-secret-not-for-production-123456789";
    private final OwnerAccountRepository accounts=mock(OwnerAccountRepository.class);
    private final OwnerSessionRepository sessions=mock(OwnerSessionRepository.class);
    private final OwnerUserDetailsService users=new OwnerUserDetailsService(accounts);
    private final SecurityConfiguration configuration=new SecurityConfiguration();
    private final PasswordEncoder passwords=configuration.passwordEncoder();
    private final AuthenticationManager manager=configuration.authenticationManager(users,passwords);
    private AuthService auth;
    private JwtDecoder decoder;
    private String hash;

    @BeforeEach void setup() {
        hash=passwords.encode("password123");
        var account=new OwnerAccount("owner@example.com",hash); account.id=42L;
        when(accounts.findByEmail("owner@example.com")).thenReturn(Optional.of(account));
        when(accounts.findById(42L)).thenReturn(Optional.of(account));
        when(sessions.save(any(OwnerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        auth=new AuthService(passwords,manager,new JwtService(configuration.jwtEncoder(SECRET)),accounts,sessions,new RefreshTokenService());
        decoder=NimbusJwtDecoder.withSecretKey(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test void normalizedLoginAuthenticatesWithBcryptAndIssuesJwtForPersistedSession() {
        var tokens=auth.login(new AuthCredentials(" Owner@Example.com ","password123"));
        var jwt=decoder.decode(tokens.accessToken());
        var session=ArgumentCaptor.forClass(OwnerSession.class);
        verify(sessions).save(session.capture());
        verify(accounts).findByEmail("owner@example.com");
        assertEquals(42L,session.getValue().ownerId);
        assertEquals("42",jwt.getSubject());
        assertEquals("hotelya",jwt.getClaimAsString("iss"));
        assertEquals(List.of("hotelya-owner"),jwt.getAudience());
        assertEquals(session.getValue().id.toString(),jwt.getClaimAsString("sid"));
        assertEquals(Duration.ofMinutes(15),Duration.between(jwt.getIssuedAt(),jwt.getExpiresAt()));
        assertEquals(900,tokens.expiresIn());
        assertEquals(new AuthUser(42L,"owner@example.com"),tokens.user());
        assertTrue(tokens.refreshToken().startsWith(session.getValue().id+"."));
        assertEquals(tokens.refreshExpiresAt(),session.getValue().expiresAt);
        assertEquals(hash,accounts.findById(42L).orElseThrow().passwordHash);
    }

    @Test void authenticationManagerErasesCredentialsAfterBcryptValidation() {
        var result=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("owner@example.com","password123"));
        assertTrue(result.isAuthenticated());
        assertEquals(42L,((OwnerPrincipal) result.getPrincipal()).ownerId());
        assertNull(result.getCredentials());
        assertNull(((OwnerPrincipal) result.getPrincipal()).getPassword());
    }

    @Test void wrongPasswordDoesNotCreateSession() {
        assertRejected("owner@example.com","wrong");
    }

    @Test void unknownAccountHasSameErrorAndDoesNotCreateSession() {
        when(accounts.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertRejected("missing@example.com","password123");
    }

    @Test void missingPasswordDoesNotCreateSession() {
        assertRejected("owner@example.com",null);
    }

    private void assertRejected(String email,String password) {
        var error=assertThrows(ApiFailure.class,() -> auth.login(new AuthCredentials(email,password)));
        assertEquals(401,error.status);
        assertEquals("Correo o contraseña incorrectos.",error.getMessage());
        verifyNoInteractions(sessions);
        verify(accounts,never()).findById(anyLong());
        verify(accounts,never()).save(any());
    }
}
