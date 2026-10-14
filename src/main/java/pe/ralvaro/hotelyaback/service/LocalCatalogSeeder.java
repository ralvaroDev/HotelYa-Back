package pe.ralvaro.hotelyaback.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.model.DetailKind;
import pe.ralvaro.hotelyaback.model.Hotel;
import pe.ralvaro.hotelyaback.model.Rate;
import pe.ralvaro.hotelyaback.model.RateMode;
import pe.ralvaro.hotelyaback.model.RoomDetail;
import pe.ralvaro.hotelyaback.model.RoomPhoto;
import pe.ralvaro.hotelyaback.model.RoomType;
import pe.ralvaro.hotelyaback.model.StayCard;
import pe.ralvaro.hotelyaback.repository.HotelRepository;
import pe.ralvaro.hotelyaback.repository.RateRepository;
import pe.ralvaro.hotelyaback.repository.RoomDetailRepository;
import pe.ralvaro.hotelyaback.repository.RoomPhotoRepository;
import pe.ralvaro.hotelyaback.repository.RoomTypeRepository;
import pe.ralvaro.hotelyaback.repository.StayCardRepository;

@Service
public class LocalCatalogSeeder {
    private final HotelRepository hotels;
    private final RoomTypeRepository rooms;
    private final StayCardRepository cards;
    private final RoomPhotoRepository photos;
    private final RoomDetailRepository details;
    private final RateRepository rates;

    public LocalCatalogSeeder(HotelRepository hotels, RoomTypeRepository rooms, StayCardRepository cards,
                              RoomPhotoRepository photos, RoomDetailRepository details, RateRepository rates) {
        this.hotels = hotels;
        this.rooms = rooms;
        this.cards = cards;
        this.photos = photos;
        this.details = details;
        this.rates = rates;
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
        cards.save(new StayCard(hotel.getId(), "Una escapada en pareja", "Haz espacio para compartir y descansar a tu ritmo.", 0));
        cards.save(new StayCard(hotel.getId(), "Una visita a la ciudad", "Consulta las condiciones y servicios para tu estadía.", 1));
        cards.save(new StayCard(hotel.getId(), "Un descanso en el camino", "Organiza tu llegada directamente con el hotel.", 2));
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
        rooms.save(type);
        photos.save(new RoomPhoto(type, image, "Tipo de habitación " + name, 0));
        details.save(new RoomDetail(type, DetailKind.RECOMMENDATION,
                "Consulta las modalidades y servicios disponibles directamente con el hotel.", 0));
        rates.save(new Rate(type, RateMode.HOUR, new BigDecimal(hourly), "PEN", null, null, true));
        rates.save(new Rate(type, RateMode.PACKAGE, new BigDecimal(pack), "PEN", 3, new BigDecimal(extra), true));
        rates.save(new Rate(type, RateMode.NIGHT, new BigDecimal(nightly), "PEN", null, null, true));
    }
}
