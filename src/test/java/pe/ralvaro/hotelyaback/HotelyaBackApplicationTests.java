package pe.ralvaro.hotelyaback;

import jakarta.persistence.EntityManager;

import javax.sql.DataSource;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.model.Hotel;
import pe.ralvaro.hotelyaback.repository.HotelRepository;
import pe.ralvaro.hotelyaback.testsupport.CatalogSeedFixture;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.flyway.default-schema=hotelya_test",
        "spring.flyway.schemas=hotelya_test",
        "spring.flyway.placeholders.catalogSchema=hotelya_test",
        "spring.jpa.properties.hibernate.default_schema=hotelya_test",
        "owner.jwt.secret=hotelya-test-secret-not-for-production-123456789",
        "owner.upload-dir=${java.io.tmpdir}/hotelya-catalog-test-uploads"
})
@Import(CatalogSeedFixture.class)
@AutoConfigureMockMvc
@Transactional
class HotelyaBackApplicationTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    CatalogSeedFixture seeder;
    @Autowired
    HotelRepository hotels;
    @Autowired
    EntityManager entities;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    DataSource datasource;

    private Long seed() {
        seeder.seed();
        entities.flush();
        entities.clear();
        return hotels.findByCode("lumis-hotel").orElseThrow().getId();
    }

    @Test
    void usesSessionPoolerAndSeparateSchema() throws Exception {
        try (var connection = datasource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().contains(":55329/"));
        }
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM hotelya_test.flyway_schema_history WHERE success AND version = '1'", Integer.class));
    }

    @Test
    void emptyCatalogIsAnEmptyArray() throws Exception {
        mvc.perform(get("/api/hotels")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void unknownHotelReturns404() throws Exception {
        mvc.perform(get("/api/hotels/99999999")).andExpect(status().isNotFound());
    }

    @Test
    void listAndDetailExposeTypesAndReferenceRates() throws Exception {
        Long id = seed();
        mvc.perform(get("/api/hotels")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Lumi's Hotel"));
        mvc.perform(get("/api/hotels/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.roomTypes.length()").value(3))
                .andExpect(jsonPath("$.roomTypes[0].name").value("Temática"))
                .andExpect(jsonPath("$.roomTypes[0].photos[0].path").value("assets/images/lumis-cuarto-tematico.jpg"))
                .andExpect(jsonPath("$.roomTypes[0].rates.length()").value(3))
                .andExpect(jsonPath("$.roomTypes[0].rates[0].amount").value(25.0))
                .andExpect(jsonPath("$.roomTypes[0].rates[1].mode").value("PACKAGE"))
                .andExpect(jsonPath("$.roomTypes[0].rates[1].packageHours").value(3))
                .andExpect(jsonPath("$.roomTypes[0].rates[1].additionalHourAmount").value(20.0))
                .andExpect(jsonPath("$.roomTypes[0].rates[2].simulated").value(true))
                .andExpect(jsonPath("$.hasParking").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.phone").value("+5117557324"))
                .andExpect(jsonPath("$.whatsapp").doesNotExist())
                .andExpect(jsonPath("$.rating").doesNotExist())
                .andExpect(jsonPath("$.availability").doesNotExist());
    }

    @Test
    void seedIsIdempotentAndKeepsEditedValues() {
        Long id = seed();
        jdbc.update("UPDATE hotelya_test.hotel SET description = ? WHERE id = ?", "Texto modificado", id);
        seeder.seed();
        entities.clear();
        assertEquals("Texto modificado", hotels.findById(id).orElseThrow().getDescription());
        assertEquals(1L, hotels.count());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM hotelya_test.room_type", Integer.class));
        assertEquals(9, jdbc.queryForObject("SELECT count(*) FROM hotelya_test.rate", Integer.class));
    }

    @Test
    void dataApiRolesHaveNoSchemaAccess() {
        for (String role : new String[]{"anon", "authenticated", "service_role"}) {
            assertEquals(false, jdbc.queryForObject("SELECT has_schema_privilege(?, 'hotelya_test', 'USAGE')", Boolean.class, role));
        }
    }

    @Test
    void allowedOriginCanReadButOtherOriginsAreRejected() throws Exception {
        for (String origin : new String[]{"http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:4173", "http://127.0.0.1:4173"}) {
            mvc.perform(get("/api/hotels").header("Origin", origin))
                    .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
        mvc.perform(get("/api/hotels").header("Origin", "https://untrusted.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/hotels")).andExpect(status().isForbidden());
    }

    @Test
    void duplicateTypeCodeIsRejectedWithinAHotel() {
        Long id = seed();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO hotelya_test.room_type (hotel_id, code, name, description, display_order) VALUES (?, 'tematica', 'Otra', 'Duplicada', 0)", id));
    }

    @Test
    void explicitSeedDoesNotRestoreDeletedRooms() {
        Long id = seed();
        jdbc.update("DELETE FROM hotelya_test.rate");
        jdbc.update("DELETE FROM hotelya_test.room_photo");
        jdbc.update("DELETE FROM hotelya_test.room_detail");
        jdbc.update("DELETE FROM hotelya_test.room_type");
        seeder.seed();
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM hotelya_test.room_type", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM hotelya_test.hotel WHERE id = ?", Integer.class, id));
    }

    private void registerSeedOwner(String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
    }

    private void assignSeedOwner(String email) throws Exception {
        jdbc.queryForObject("SELECT set_config('hotelya.seed.owner_email', ?, true)", String.class, email);
        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of("scripts/local/assign-owner.sql")));
    }

    @Test
    void explicitAssignmentUsesRegisteredAccountAndKeepsPasswordOnRepeat() throws Exception {
        Long hotelId = seed();
        registerSeedOwner("owner@example.com");
        String hash = jdbc.queryForObject("SELECT password_hash FROM hotelya_test.owner_account", String.class);
        assignSeedOwner(" Owner@Example.com ");
        assignSeedOwner("owner@example.com");
        assertEquals(jdbc.queryForObject("SELECT id FROM hotelya_test.owner_account", Long.class),
                jdbc.queryForObject("SELECT owner_id FROM hotelya_test.hotel WHERE id = ?", Long.class, hotelId));
        assertEquals(hash, jdbc.queryForObject("SELECT password_hash FROM hotelya_test.owner_account", String.class));
    }

    @Test
    void explicitAssignmentCannotReplaceAnExistingOwner() throws Exception {
        seed();
        registerSeedOwner("first@example.com");
        registerSeedOwner("second@example.com");
        assignSeedOwner("first@example.com");
        assertThrows(org.springframework.dao.DataAccessException.class, () -> assignSeedOwner("second@example.com"));
    }

}
