package pe.ralvaro.hotelyaback.infrastructure;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.domain.*;

@Service
public class LocalCatalogSeeder {
    private final HotelRepository hotels;
    private final EntityManager entities;
    public LocalCatalogSeeder(HotelRepository hotels, EntityManager entities) {
        this.hotels = hotels;
        this.entities = entities;
    }
    @Transactional
    public void seed() {
        // An existing hotel is authoritative, including deliberate removals and edits to its catalog.
        if (hotels.findByCode("lumis-hotel").isPresent()) return;
        var hotel = hotels.save(new Hotel("lumis-hotel", "Lumi's Hotel",
            "Te damos la bienvenida a Lumi's Hotel, ubicado en Av. Lizardo Montero 161, en San Juan de Miraflores, Lima. Un punto de partida para una visita al sur de la ciudad o una pausa en tu día.",
            "Av. Lizardo Montero 161, San Juan de Miraflores, Lima, Perú.",
            "San Juan de Miraflores, Lima", "+5117557324",
            "assets/images/lumis-hotel-logo.jpg", "assets/images/lumis-hotel-fachada.jpg",
            "assets/videos/lumis-promocional.mp4"));
        entities.persist(new StayCard(hotel.getId(), "Una escapada en pareja", "Haz espacio para compartir y descansar a tu ritmo.", 0));
        entities.persist(new StayCard(hotel.getId(), "Una visita a la ciudad", "Consulta las condiciones y servicios para tu estadía.", 1));
        entities.persist(new StayCard(hotel.getId(), "Un descanso en el camino", "Organiza tu llegada directamente con el hotel.", 2));
        addType(hotel, "tematica", "Temática",
            "Un ambiente especial para celebrar en pareja, con detalles románticos. Consulta las opciones de decoración para tu ocasión.",
            "assets/images/lumis-cuarto-tematico.jpg", 0, "25", "60", "20", "120");
        addType(hotel, "matrimonial", "Matrimonial",
            "Un espacio acogedor con cama matrimonial, iluminación cálida y televisión para disfrutar de una estadía en pareja.",
            "assets/images/lumis-cuarto-matrimonial.jpg", 1, "20", "50", "15", "100");
        addType(hotel, "premium", "Premium",
            "Disfruta de una habitación con jacuzzi y un rincón para relajarte, ideal para una escapada especial.",
            "assets/images/lumis-cuarto-premium.jpg", 2, "35", "90", "25", "180");
    }
    private void addType(Hotel hotel, String code, String name, String description, String image,
            int order, String hourly, String pack, String extra, String nightly) {
        var type = new RoomType(hotel, code, name, description, order);
        entities.persist(type);
        entities.persist(new RoomPhoto(type, image, "Tipo de habitación " + name, 0));
        entities.persist(new RoomDetail(type, DetailKind.RECOMMENDATION,
            "Consulta las modalidades y servicios disponibles directamente con el hotel.", 0));
        entities.persist(new Rate(type, RateMode.HOUR, new BigDecimal(hourly), "PEN", null, null, true));
        entities.persist(new Rate(type, RateMode.PACKAGE, new BigDecimal(pack), "PEN", 3, new BigDecimal(extra), true));
        entities.persist(new Rate(type, RateMode.NIGHT, new BigDecimal(nightly), "PEN", null, null, true));
    }
}
