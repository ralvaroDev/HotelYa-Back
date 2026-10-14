package pe.ralvaro.hotelyaback.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import pe.ralvaro.hotelyaback.model.*;
import pe.ralvaro.hotelyaback.repository.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTests {
    private final HotelRepository hotels = mock(HotelRepository.class);
    private final RoomTypeRepository rooms = mock(RoomTypeRepository.class);
    private final StayCardRepository cards = mock(StayCardRepository.class);
    private final CatalogService service = new CatalogService(hotels, rooms, cards);

    private Hotel hotel() {
        return new Hotel("hotel", "Hotel", "Descripción", "Dirección", "Lima", null, null, null, null);
    }

    @Test
    void emptyCatalogReturnsAnEmptyList() {
        assertTrue(service.list().isEmpty());
    }

    @Test
    void publicListOmitsDraftsAndRequestsStableOrdering() {
        var published = hotel();
        var draft = hotel();
        draft.setPublished(false);
        when(hotels.findAll(Sort.by("name", "id"))).thenReturn(List.of(draft, published));

        var result = service.list();

        assertEquals(1, result.size());
        assertEquals("Hotel", result.getFirst().name());
    }

    @Test
    void unknownHotelReturns404() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.detail(9L)).getStatusCode().value());
        verifyNoInteractions(rooms, cards);
    }

    @Test
    void draftsAreHiddenFromThePublicButAvailableInTheOwnerView() {
        var draft = hotel();
        draft.setPublished(false);
        when(hotels.findById(9L)).thenReturn(Optional.of(draft));

        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.detail(9L)).getStatusCode().value());
        assertEquals("Hotel", service.ownerDetail(9L).name());
    }

    @Test
    void detailMapsRoomQuantitiesPhotosRatesAndPresentation() {
        var hotel = hotel();
        var room = new RoomType(hotel, "suite", "Suite", "Con baño", 0);
        room.update("Suite", "Con baño", 0, 3, 1);
        room.getPhotos().add(new RoomPhoto(room, "assets/images/suite.jpg", "Suite", 0));
        room.getDetails().add(new RoomDetail(room, DetailKind.FEATURE, "WiFi", 0));
        room.getRates().add(new Rate(room, RateMode.HOUR, new BigDecimal("25.00"), "PEN", null, null, false));
        when(hotels.findById(9L)).thenReturn(Optional.of(hotel));
        when(rooms.findByHotelIdOrderByDisplayOrderAscIdAsc(9L)).thenReturn(List.of(room));
        when(cards.findByHotelIdOrderByDisplayOrderAscIdAsc(9L)).thenReturn(List.of(new StayCard(9L, "Descansa", "Bienvenido", 0)));

        var result = service.detail(9L);

        assertEquals("Lima", result.location());
        assertNull(result.phone());
        assertEquals("Descansa", result.stayCards().getFirst().title());
        var type = result.roomTypes().getFirst();
        assertEquals(3, type.totalUnits());
        assertEquals(1, type.availableUnits());
        assertEquals("assets/images/suite.jpg", type.photos().getFirst().path());
        assertEquals("WiFi", type.details().getFirst().text());
        assertEquals(new BigDecimal("25.00"), type.rates().getFirst().amount());
        assertFalse(type.rates().getFirst().simulated());
    }
}
