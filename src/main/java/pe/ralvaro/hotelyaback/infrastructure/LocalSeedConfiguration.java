package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
@ConditionalOnProperty(name = "catalog.seed.enabled", havingValue = "true")
public class LocalSeedConfiguration {
    @Bean
    ApplicationRunner seedLocalCatalog(LocalCatalogSeeder seeder) {
        return args -> seeder.seed();
    }
}
