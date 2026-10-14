package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CatalogConfiguration implements WebMvcConfigurer {
    private final String[] origins;
    public CatalogConfiguration(@Value("${catalog.cors.allowed-origins}") String[] origins) {
        this.origins = origins;
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET").maxAge(3600);
    }
}
