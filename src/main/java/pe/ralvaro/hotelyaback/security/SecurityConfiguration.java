package pe.ralvaro.hotelyaback.security;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.web.cors.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    private SecretKeySpec key(String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalArgumentException("HOTELYA_JWT_SECRET debe contener al menos 32 bytes.");
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(@Value("${owner.jwt.secret}") String secret) { return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret))); }
    @Bean JwtDecoder jwtDecoder(@Value("${owner.jwt.secret}") String secret,ObjectProvider<AuthService> auth) {
        var decoder=NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audience=jwt -> jwt.getAudience().contains("hotelya-owner") ? OAuth2TokenValidatorResult.success() : invalid();
        OAuth2TokenValidator<Jwt> session=jwt -> auth.getObject().active(jwt) ? OAuth2TokenValidatorResult.success() : invalid();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("hotelya"),audience,session));
        return decoder;
    }
    private static OAuth2TokenValidatorResult invalid() { return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Sesión no válida.",null)); }
    @Bean SecurityFilterChain security(HttpSecurity http, @Value("${owner.cookie.secure:false}") boolean secure, @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource source) throws Exception {
        var csrf=new CookieCsrfTokenRepository(); csrf.setCookiePath("/api/auth");
        csrf.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(secure).sameSite(secure ? "None" : "Lax"));
        return http.cors(c -> c.configurationSource(source)).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c.csrfTokenRepository(csrf).ignoringRequestMatchers("/api/owner/**"))
            .authorizeHttpRequests(a -> a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll().requestMatchers("/api/owner/**","/api/auth/me").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/hotels/**","/api/media/**","/api/auth/csrf").permitAll()
                .requestMatchers("/api/auth/register","/api/auth/login","/api/auth/refresh","/api/auth/logout").permitAll()
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> {}).authenticationEntryPoint((req,res,e) -> {
                res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Inicia sesión para continuar.\",\"fields\":{}}");
            }))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,x) -> { res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Inicia sesión para continuar.\",\"fields\":{}}"); })
                .accessDeniedHandler((req,res,x) -> { res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"La solicitud no está autorizada.\",\"fields\":{}}"); }))
            .build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${catalog.cors.allowed-origins}") String[] origins) {
        var config=new CorsConfiguration(); config.setAllowedOrigins(List.of(origins)); config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","X-XSRF-TOKEN")); config.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",config); return source;
    }
}
