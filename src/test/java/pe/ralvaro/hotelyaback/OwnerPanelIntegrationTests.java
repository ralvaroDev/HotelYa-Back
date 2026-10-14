package pe.ralvaro.hotelyaback;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import jakarta.servlet.http.Cookie;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@Tag("integration")
@SpringBootTest(properties={
    "spring.flyway.default-schema=hotelya_test", "spring.flyway.schemas=hotelya_test", "spring.flyway.placeholders.catalogSchema=hotelya_test",
    "spring.jpa.properties.hibernate.default_schema=hotelya_test", "catalog.seed.enabled=false",
    "owner.jwt.secret=hotelya-test-secret-not-for-production-123456789", "owner.upload-dir=${java.io.tmpdir}/hotelya-owner-test-uploads"
}) @AutoConfigureMockMvc
class OwnerPanelIntegrationTests {
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired JwtEncoder encoder; @Autowired JwtDecoder decoder;
    record Session(String token,Cookie cookie) {}
    private Session session(MvcResult result) throws Exception {
        String cookie=result.getResponse().getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("HOTELYA_REFRESH=")).findFirst().orElseThrow();
        return new Session(JsonPath.read(result.getResponse().getContentAsString(),"$.accessToken"),MockCookie.parse(cookie));
    }
    private Session register(String email) throws Exception { return session(mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\"password123\"}")).andExpect(status().isCreated()).andReturn()); }
    private final String hotel="""
        {"name":"Hotel de prueba","description":"Descripción","address":"Dirección de prueba","location":"Lima"}
        """;
    private void hotel(Session s) throws Exception { mvc.perform(post("/api/owner/hotel").header("Authorization","Bearer "+s.token()).contentType("application/json").content(hotel)).andExpect(status().isCreated()); }
    private String room(String path,int available) { return """
        {"name":"Matrimonial","description":"Habitación de prueba","displayOrder":0,"totalUnits":3,"availableUnits":%d,
        "photos":%s,"details":[{"kind":"FEATURE","text":"Cama matrimonial"}],
        "rates":[{"mode":"HOUR","amount":25,"currency":"PEN","packageHours":null,"additionalHourAmount":null,"simulated":false}]}
        """.formatted(available,path==null?"[]":"[{\"path\":\""+path+"\",\"altText\":\"Habitación\"}]"); }
    private byte[] png() throws Exception { var stream=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",stream); return stream.toByteArray(); }
    private MvcResult upload(Session s) throws Exception { return mvc.perform(multipart("/api/owner/media").file(new MockMultipartFile("file","foto.png","image/png",png())).header("Authorization","Bearer "+s.token())).andExpect(status().isCreated()).andReturn(); }
    @AfterEach void cleanIsolatedSchema() throws Exception {
        for(String id:jdbc.queryForList("select id::text from hotelya_test.media_file",String.class)) Files.deleteIfExists(Path.of(System.getProperty("java.io.tmpdir"),"hotelya-owner-test-uploads",id));
        for(String table:List.of("stay_card","media_file","rate","room_detail","room_photo","room_type","hotel","owner_session","owner_account")) jdbc.update("delete from hotelya_test."+table);
    }
    @Test void csrfAndUnauthorizedAccessAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/owner/hotel")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void duplicateWrongPasswordAndNormalizedEmail() throws Exception {
        var s=register("Owner@Example.com");
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+s.token())).andExpect(jsonPath("$.email").value("owner@example.com"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"email\":\"owner@example.com\",\"password\":\"password123\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.fields.email").exists());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"owner@example.com\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"owner@example.com\",\"password\":\"password123\"}")).andExpect(status().isOk());
        assertFalse(jdbc.queryForObject("select password_hash from hotelya_test.owner_account",String.class).contains("password123"));
    }
    @Test void refreshRotatesReplayRevokesAndLogoutInvalidatesAccess() throws Exception {
        var s=register("owner@example.com");
        var rotated=session(mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(s.cookie())).andExpect(status().isOk()).andReturn());
        assertNotEquals(s.cookie().getValue(),rotated.cookie().getValue());
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(s.cookie())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+rotated.token())).andExpect(status().isUnauthorized());
        var next=session(mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"owner@example.com\",\"password\":\"password123\"}")).andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/auth/logout").with(csrf()).cookie(next.cookie())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+next.token())).andExpect(status().isUnauthorized());
    }
    @Test void expiredJwtAndRefreshAreRejected() throws Exception {
        var s=register("owner@example.com"); var original=decoder.decode(s.token());
        var claims=JwtClaimsSet.builder().issuer("hotelya").subject(original.getSubject()).audience(List.of("hotelya-owner")).claim("sid",original.getClaimAsString("sid")).issuedAt(Instant.now().minusSeconds(3600)).expiresAt(Instant.now().minusSeconds(1800)).build();
        var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        jdbc.update("update hotelya_test.owner_session set expires_at=now()-interval '1 day'");
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(s.cookie())).andExpect(status().isUnauthorized());
    }
}
