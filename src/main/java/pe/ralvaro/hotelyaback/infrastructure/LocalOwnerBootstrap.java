package pe.ralvaro.hotelyaback.infrastructure;

import pe.ralvaro.hotelyaback.application.auth.AuthService;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration @Profile("local")
public class LocalOwnerBootstrap {
    @Bean ApplicationRunner bootstrapOwner(AuthService auth,LocalCatalogSeeder seed,HotelRepository hotels, PlatformTransactionManager manager,
        @Value("${owner.bootstrap.email:}") String email,@Value("${owner.bootstrap.password:}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) return;
            if (email.isBlank() || password.isBlank()) throw new IllegalArgumentException("Configura ambos valores HOTELYA_LUMIS_OWNER_EMAIL y HOTELYA_LUMIS_OWNER_PASSWORD.");
            seed.seed();
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                var hotel=hotels.findByCode("lumis-hotel").orElseThrow();
                if (hotel.getOwnerId()!=null) {
                    if (!auth.user(hotel.getOwnerId()).email().equals(auth.normalizedEmail(email)))
                        throw new IllegalStateException("Lumi's ya tiene otra cuenta asociada. La configuración inicial no cambia su dueño.");
                    return;
                }
                var account=auth.bootstrap(email,password);
                if (hotels.findByOwnerId(account.id).isPresent()) throw new IllegalStateException("La cuenta inicial ya tiene otro hotel asociado.");
                hotel.assignOwner(account.id); hotels.save(hotel);
            });
        };
    }
}
