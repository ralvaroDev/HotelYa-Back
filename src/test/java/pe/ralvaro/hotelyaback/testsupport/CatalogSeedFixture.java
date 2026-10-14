package pe.ralvaro.hotelyaback.testsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TestComponent
public class CatalogSeedFixture {
    private final JdbcTemplate jdbc;

    public CatalogSeedFixture(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void seed() {
        try {
            jdbc.execute("SET LOCAL search_path TO hotelya_test");
            jdbc.execute(Files.readString(Path.of("scripts/local/seed-catalog.sql")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
