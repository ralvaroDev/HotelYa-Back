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
    @Test void ownerCannotUseAnotherOwnersRoomOrFiles() throws Exception {
        var a=register("a@example.com"); hotel(a); var uploaded=upload(a);
        String path=JsonPath.read(uploaded.getResponse().getContentAsString(),"$.path"), mediaId=JsonPath.read(uploaded.getResponse().getContentAsString(),"$.id");
        var saved=mvc.perform(post("/api/owner/hotel/room-types").header("Authorization","Bearer "+a.token()).contentType("application/json").content(room(path,2))).andExpect(status().isCreated()).andReturn();
        Number id=JsonPath.read(saved.getResponse().getContentAsString(),"$.detail.roomTypes[0].id");
        var b=register("b@example.com"); hotel(b);
        mvc.perform(put("/api/owner/hotel/room-types/"+id).header("Authorization","Bearer "+b.token()).contentType("application/json").content(room(null,1))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/owner/hotel/room-types/"+id).header("Authorization","Bearer "+b.token())).andExpect(status().isNotFound());
        mvc.perform(post("/api/owner/hotel/room-types").header("Authorization","Bearer "+b.token()).contentType("application/json").content(room(path,1))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/owner/media/"+mediaId).header("Authorization","Bearer "+b.token())).andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/owner/media/"+mediaId).header("Authorization","Bearer "+b.token())).andExpect(status().isNotFound());
    }
    @Test void fullDraftPublicationEditingHidingAndDeletionFlow() throws Exception {
        var s=register("owner@example.com"); hotel(s);
        mvc.perform(get("/api/hotels")).andExpect(content().json("[]"));
        mvc.perform(put("/api/owner/hotel/publication").header("Authorization","Bearer "+s.token()).contentType("application/json").content("{\"published\":true}")).andExpect(status().isConflict());
        var uploaded=upload(s); String path=JsonPath.read(uploaded.getResponse().getContentAsString(),"$.path"), mediaId=JsonPath.read(uploaded.getResponse().getContentAsString(),"$.id");
        mvc.perform(post("/api/owner/hotel/room-types").header("Authorization","Bearer "+s.token()).contentType("application/json").content(room(path,4))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.availableUnits").exists());
        var saved=mvc.perform(post("/api/owner/hotel/room-types").header("Authorization","Bearer "+s.token()).contentType("application/json").content(room(path,2))).andExpect(status().isCreated()).andReturn();
        Number roomId=JsonPath.read(saved.getResponse().getContentAsString(),"$.detail.roomTypes[0].id"), hotelId=JsonPath.read(saved.getResponse().getContentAsString(),"$.detail.id");
        mvc.perform(put("/api/owner/hotel/stay-cards").header("Authorization","Bearer "+s.token()).contentType("application/json").content("[{\"title\":\"Ven a descansar\",\"description\":\"Te esperamos\"}]")).andExpect(status().isOk());
        mvc.perform(put("/api/owner/hotel/publication").header("Authorization","Bearer "+s.token()).contentType("application/json").content("{\"published\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/hotels/"+hotelId)).andExpect(status().isOk()).andExpect(jsonPath("$.roomTypes[0].availableUnits").value(2)).andExpect(jsonPath("$.stayCards[0].title").value("Ven a descansar"));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(delete("/api/owner/hotel/room-types/"+roomId).header("Authorization","Bearer "+s.token())).andExpect(status().isConflict());
        mvc.perform(get("/api/hotels/"+hotelId)).andExpect(jsonPath("$.roomTypes.length()").value(1));
        mvc.perform(put("/api/owner/hotel/room-types/"+roomId).header("Authorization","Bearer "+s.token()).contentType("application/json").content(room(path,1))).andExpect(status().isOk());
        mvc.perform(delete("/api/owner/media/"+mediaId).header("Authorization","Bearer "+s.token())).andExpect(status().isConflict());
        mvc.perform(put("/api/owner/hotel/publication").header("Authorization","Bearer "+s.token()).contentType("application/json").content("{\"published\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/hotels/"+hotelId)).andExpect(status().isNotFound()); mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/owner/hotel/room-types/"+roomId).header("Authorization","Bearer "+s.token())).andExpect(status().isOk());
        mvc.perform(delete("/api/owner/media/"+mediaId).header("Authorization","Bearer "+s.token())).andExpect(status().isNoContent());
        assertFalse(Files.exists(Path.of(System.getProperty("java.io.tmpdir"),"hotelya-owner-test-uploads",mediaId)));
    }
    @Test void legacyPublishedHotelCanSaveInformationAndConfigureRoomsProgressively() throws Exception {
        var s=register("legacy@example.com"); hotel(s);
        String path=JsonPath.read(upload(s).getResponse().getContentAsString(),"$.path");
        for(int i=0;i<3;i++) mvc.perform(post("/api/owner/hotel/room-types").header("Authorization","Bearer "+s.token()).contentType("application/json").content(room(path,2))).andExpect(status().isCreated());
        Long id=jdbc.queryForObject("select id from hotelya_test.hotel",Long.class);
        jdbc.update("update hotelya_test.hotel set published=true where id=?",id);
        jdbc.update("update hotelya_test.room_type set total_units=null, available_units=null where hotel_id=?",id);
        String information=hotel.replace("Hotel de prueba","Hotel actualizado");
        mvc.perform(put("/api/owner/hotel").header("Authorization","Bearer "+s.token()).contentType("application/json").content(information)).andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true)).andExpect(jsonPath("$.detail.roomTypes.length()").value(3));
        mvc.perform(get("/api/hotels/"+id)).andExpect(jsonPath("$.name").value("Hotel actualizado"));
        mvc.perform(put("/api/owner/hotel/stay-cards").header("Authorization","Bearer "+s.token()).contentType("application/json").content("[]")).andExpect(status().isOk());
        Long roomId=jdbc.queryForObject("select min(id) from hotelya_test.room_type where hotel_id=?",Long.class,id);
        mvc.perform(put("/api/owner/hotel/room-types/"+roomId).header("Authorization","Bearer "+s.token()).contentType("application/json").content(room(path,1))).andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true));
        mvc.perform(delete("/api/owner/hotel/room-types/"+roomId).header("Authorization","Bearer "+s.token())).andExpect(status().isConflict());
    }
    @Test void fakeUploadsAndInvalidRatesAreRejected() throws Exception {
        var s=register("owner@example.com"); hotel(s);
        mvc.perform(multipart("/api/owner/media").file(new MockMultipartFile("file","fake.png","image/png","<script>bad</script>".getBytes())).header("Authorization","Bearer "+s.token())).andExpect(status().isBadRequest());
        byte[] oversized=new byte[10*1024*1024+1]; byte[] valid=png(); System.arraycopy(valid,0,oversized,0,valid.length);
        mvc.perform(multipart("/api/owner/media").file(new MockMultipartFile("file","large.png","image/png",oversized)).header("Authorization","Bearer "+s.token())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.file").exists());
        mvc.perform(post("/api/owner/hotel/room-types").header("Authorization","Bearer "+s.token()).contentType("application/json").content(room(null,1).replace("\"amount\":25","\"amount\":-1"))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields['rates.0.amount']").exists());
        mvc.perform(post("/api/owner/hotel").header("Authorization","Bearer "+s.token()).contentType("application/json").content(hotel)).andExpect(status().isConflict());
    }
}
