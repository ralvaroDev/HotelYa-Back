package pe.ralvaro.hotelyaback.application;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.ralvaro.hotelyaback.infrastructure.HotelRepository;
import pe.ralvaro.hotelyaback.infrastructure.RoomTypeRepository;
import static pe.ralvaro.hotelyaback.application.CatalogDtos.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final HotelRepository hotels;
    private final RoomTypeRepository roomTypes;
    public CatalogService(HotelRepository hotels, RoomTypeRepository roomTypes) {
        this.hotels = hotels;
        this.roomTypes = roomTypes;
    }
    public List<HotelSummary> list() {
        return hotels.findAll(Sort.by("name", "id")).stream().map(h ->
            new HotelSummary(h.getId(), h.getName(), h.getDescription(), h.getLocation(), h.getLogoPath())).toList();
    }
    public HotelDetail detail(Long id) {
        var h = hotels.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel no encontrado"));
        var types = roomTypes.findByHotelIdOrderByDisplayOrderAscIdAsc(id).stream().map(r ->
            new RoomTypeDetail(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                r.getPhotos().stream().map(p -> new Photo(p.getPath(), p.getAltText())).toList(),
                r.getDetails().stream().map(d -> new Detail(d.getKind(), d.getText())).toList(),
                r.getRates().stream().map(t -> new RateInfo(t.getMode(), t.getAmount(), t.getCurrency(),
                    t.getPackageHours(), t.getAdditionalHourAmount(), t.getSimulated())).toList())).toList();
        return new HotelDetail(h.getId(), h.getName(), h.getDescription(), h.getAddress(), h.getLocation(),
            h.getPhone(), h.getLogoPath(), h.getFacadePath(), h.getVideoPath(), h.getLatitude(),
            h.getLongitude(), h.getWhatsapp(), h.getOpeningHours(), h.getCheckInOutConditions(),
            h.getHasParking(), types);
    }
}
