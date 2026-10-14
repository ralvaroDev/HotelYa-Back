package pe.ralvaro.hotelyaback.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.ralvaro.hotelyaback.model.Hotel;
import pe.ralvaro.hotelyaback.repository.HotelRepository;
import pe.ralvaro.hotelyaback.repository.RoomTypeRepository;
import pe.ralvaro.hotelyaback.repository.StayCardRepository;

import static pe.ralvaro.hotelyaback.dto.catalog.CatalogDtos.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final HotelRepository hotels;
    private final RoomTypeRepository roomTypes;
    private final StayCardRepository cards;

    public CatalogService(HotelRepository hotels, RoomTypeRepository roomTypes, StayCardRepository cards) {
        this.hotels = hotels;
        this.roomTypes = roomTypes;
        this.cards = cards;
    }

    public List<HotelSummary> list() {
        return hotels.findAll(Sort.by("name", "id")).stream().filter(h -> h.isPublished()).map(h ->
                new HotelSummary(h.getId(), h.getName(), h.getDescription(), h.getLocation(), h.getLogoPath())).toList();
    }

    public HotelDetail detail(Long id) {
        return detail(id, false);
    }

    public HotelDetail ownerDetail(Long id) {
        return detail(id, true);
    }

    private HotelDetail detail(Long id, boolean privateView) {
        var h = hotels.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel no encontrado"));
        if (!privateView && !h.isPublished())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel no encontrado");
        var types = roomTypes.findByHotelIdOrderByDisplayOrderAscIdAsc(id).stream().map(r ->
                new RoomTypeDetail(r.getId(), r.getCode(), r.getName(), r.getDescription(),
                        r.getPhotos().stream().map(p -> new Photo(p.getPath(), p.getAltText())).toList(),
                        r.getDetails().stream().map(d -> new Detail(d.getKind(), d.getText())).toList(),
                        r.getRates().stream().map(t -> new RateInfo(t.getMode(), t.getAmount(), t.getCurrency(),
                                t.getPackageHours(), t.getAdditionalHourAmount(), t.getSimulated())).toList(), r.getTotalUnits(), r.getAvailableUnits())).toList();
        return new HotelDetail(h.getId(), h.getName(), h.getDescription(), h.getAddress(), h.getLocation(),
                h.getPhone(), h.getLogoPath(), h.getFacadePath(), h.getVideoPath(), h.getLatitude(),
                h.getLongitude(), h.getOpeningHours(), h.getCheckInOutConditions(),
                h.getHasParking(), h.getMapUrl(), h.getOpeningTime(), h.getClosingTime(), types, cards.findByHotelIdOrderByDisplayOrderAscIdAsc(id).stream().map(c -> new StayCardInfo(c.getTitle(), c.getDescription())).toList());
    }
}
