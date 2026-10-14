package pe.ralvaro.hotelyaback;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.flyway.default-schema=hotelya_test", "spring.flyway.schemas=hotelya_test",
        "spring.flyway.placeholders.catalogSchema=hotelya_test",
        "spring.jpa.properties.hibernate.default_schema=hotelya_test",
        "owner.jwt.secret=hotelya-test-secret-not-for-production-123456789",
        "owner.upload-dir=${java.io.tmpdir}/hotelya-startup-test-uploads",
        "catalog.seed.enabled=true", "owner.bootstrap.email=ignored@example.com", "owner.bootstrap.password=password123"
})
class NoAutomaticSeedIntegrationTests {
    @Autowired
    ApplicationContext context;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void localStartupDoesNotCreateExampleDataEvenWithLegacySettings() {
        assertFalse(context.containsBean("bootstrapOwner"));
        assertFalse(context.containsBean("seedLocalCatalog"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM hotelya_test.hotel", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM hotelya_test.owner_account", Integer.class));
    }
}
