package pe.ralvaro.hotelyaback.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import pe.ralvaro.hotelyaback.dto.request.*;
import pe.ralvaro.hotelyaback.exception.ApiFailure;
import pe.ralvaro.hotelyaback.model.*;
import pe.ralvaro.hotelyaback.repository.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pe.ralvaro.hotelyaback.dto.catalog.CatalogDtos.*;

class OwnerHotelServiceTests {
    private final HotelRepository hotels = mock(HotelRepository.class);
    private final RoomTypeRepository rooms = mock(RoomTypeRepository.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final StayCardRepository cards = mock(StayCardRepository.class);
    private final RoomPhotoRepository photos = mock(RoomPhotoRepository.class);
    private final RoomDetailRepository details = mock(RoomDetailRepository.class);
    private final RateRepository rates = mock(RateRepository.class);
    private final MediaFileRepository media = mock(MediaFileRepository.class);
    private final OwnerHotelService service = new OwnerHotelService(hotels, rooms, catalog, cards, photos, details, rates, media);
    private Hotel hotel;

    @BeforeEach
    void setup() {
        hotel = spy(new Hotel("hotel", "Hotel", "Descripción", "Dirección", "Lima", null, null, null, null));
        doReturn(10L).when(hotel).getId();
        hotel.assignOwner(42L);
        hotel.setPublished(false);
        when(hotels.findByOwnerId(42L)).thenReturn(Optional.of(hotel));
        when(hotels.findByOwnerIdForUpdate(42L)).thenReturn(Optional.of(hotel));
    }

    private HotelWrite information(String phone, String map, LocalTime start, LocalTime end) {
        return new HotelWrite(" Hotel editado ", " Descripción ", " Dirección ", " Lima ", phone,
                null, null, null, null, null, null, null, true, map, start, end);
    }

    private RoomWrite room(Integer total, Integer available, List<RoomRateWrite> prices) {
        return new RoomWrite(" Suite ", " Descripción ", 0, total, available, null, List.of(), List.of(), prices);
    }

    private RoomRateWrite rate(RateMode mode, String amount, Integer hours, String currency) {
        return new RoomRateWrite(mode, new BigDecimal(amount), hours, currency);
    }

    private void assertField(String field, Runnable action) {
        var error = assertThrows(ApiFailure.class, action::run);
        assertEquals(400, error.status);
        assertTrue(error.fields.containsKey(field), error.fields.toString());
    }

    @Test
    void missingOwnerHotelReturns404() {
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.owned(99L, false)).status);
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.owned(99L, true)).status);
    }

    @Test
    void aSecondHotelIsRejected() {
        assertEquals(409, assertThrows(ApiFailure.class,
                () -> service.create(42L, information(null, null, null, null))).status);
        verify(hotels, never()).save(any());
    }

    @Test
    void aNewHotelBelongsToTheOwnerAndStartsAsADraft() {
        when(hotels.findByOwnerId(42L)).thenReturn(Optional.empty()).thenReturn(Optional.of(hotel));
        service.create(42L, information(null, null, null, null));
        var saved = ArgumentCaptor.forClass(Hotel.class);
        verify(hotels).save(saved.capture());
        assertEquals(42L, saved.getValue().getOwnerId());
        assertFalse(saved.getValue().isPublished());
        assertEquals("Hotel editado", saved.getValue().getName());
    }

    @Test
    void informationIsTrimmedAndOvernightHoursArePreserved() {
        service.update(42L, information("+51987654321", "https://maps.example.com/place", LocalTime.of(20, 0), LocalTime.of(6, 0)));
        assertEquals("Hotel editado", hotel.getName());
        assertEquals("+51987654321", hotel.getPhone());
        assertEquals("https://maps.example.com/place", hotel.getMapUrl());
        assertEquals("20:00 – 06:00 (día siguiente)", hotel.getOpeningHours());
        assertEquals(true, hotel.getHasParking());
        verify(hotels).flush();
    }

    @Test
    void equalOpeningAndClosingTimesMean24Hours() {
        service.update(42L, information(null, null, LocalTime.NOON, LocalTime.NOON));
        assertEquals("24 horas", hotel.getOpeningHours());
    }

    @ParameterizedTest
    @ValueSource(strings = {"987654321", "+51987 654321", "+5198765432", "+519876543210"})
    void invalidPhoneNumbersAreRejected(String phone) {
        assertField("phone", () -> service.update(42L, information(phone, null, null, null)));
        verify(hotels, never()).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.com", "https://user:pass@example.com", "not a url"})
    void mapMustBeAnHttpsUrlWithoutCredentials(String map) {
        assertField("mapUrl", () -> service.update(42L, information(null, map, null, null)));
    }

    @Test
    void openingAndClosingTimesMustBeProvidedTogether() {
        assertField("closingTime", () -> service.update(42L, information(null, null, LocalTime.NOON, null)));
        assertField("openingTime", () -> service.update(42L, information(null, null, null, LocalTime.NOON)));
    }

    @Test
    void publicationRequiresACompleteRoomType() {
        assertEquals(409, assertThrows(ApiFailure.class, () -> service.publication(42L, true)).status);
        assertFalse(hotel.isPublished());
        verify(hotels, never()).flush();
    }

    @Test
    void completeHotelCanBePublishedAndHidden() {
        var room = new RoomType(hotel, "suite", "Suite", "Descripción", 0);
        room.update("Suite", "Descripción", 0, 2, 1);
        room.getPhotos().add(new RoomPhoto(room, "assets/images/suite.jpg", "Suite", 0));
        room.getRates().add(new Rate(room, RateMode.HOUR, BigDecimal.TEN, "PEN", null, null, false));
        when(rooms.findByHotelIdOrderByDisplayOrderAscIdAsc(10L)).thenReturn(List.of(room));
        assertTrue(service.publication(42L, true).published());
        assertFalse(service.publication(42L, false).published());
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "3,-1", "3,4"})
    void availabilityMustBeBetweenZeroAndTotal(int total, int available) {
        assertField("availableUnits", () -> service.saveRoom(42L, null, room(total, available, List.of())));
        verify(rooms, never()).save(any());
    }

    @Test
    void roomQuantitiesAreRequired() {
        assertField("availableUnits", () -> service.saveRoom(42L, null, room(null, 0, List.of())));
        assertField("availableUnits", () -> service.saveRoom(42L, null, room(3, null, List.of())));
    }

    @Test
    void roomSavesRealRatesInSolesAndTrimsText() {
        service.saveRoom(42L, null, room(3, 0, List.of(rate(RateMode.PACKAGE, "60.00", 3, null))));
        var savedRoom = ArgumentCaptor.forClass(RoomType.class);
        verify(rooms).save(savedRoom.capture());
        assertEquals("Suite", savedRoom.getValue().getName());
        assertEquals(0, savedRoom.getValue().getAvailableUnits());
        var savedRate = ArgumentCaptor.forClass(Rate.class);
        verify(rates).save(savedRate.capture());
        assertEquals("PEN", savedRate.getValue().getCurrency());
        assertFalse(savedRate.getValue().getSimulated());
        assertEquals(3, savedRate.getValue().getPackageHours());
        assertEquals(new BigDecimal("60.00"), savedRate.getValue().getAmount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.001", "10000000000"})
    void pricesMustBePositiveAndFitTheSupportedPrecision(String amount) {
        assertField("rates.0.amount", () -> service.saveRoom(42L, null,
                room(3, 1, List.of(rate(RateMode.HOUR, amount, null, "PEN")))));
        verify(rates, never()).save(any());
    }

    @Test
    void foreignCurrenciesAreRejected() {
        assertField("rates.0.currency", () -> service.saveRoom(42L, null,
                room(3, 1, List.of(rate(RateMode.HOUR, "10", null, "USD")))));
    }

    @Test
    void eachRateModeCanAppearOnlyOnce() {
        var price = rate(RateMode.HOUR, "25", null, "PEN");
        assertField("rates.1.mode", () -> service.saveRoom(42L, null, room(3, 1, List.of(price, price))));
    }

    @Test
    void onlyPackagesAcceptAPositiveDuration() {
        assertField("rates.0.packageHours", () -> service.saveRoom(42L, null,
                room(3, 1, List.of(rate(RateMode.PACKAGE, "60", 0, "PEN")))));
        assertField("rates.0.mode", () -> service.saveRoom(42L, null,
                room(3, 1, List.of(rate(RateMode.NIGHT, "100", 3, "PEN")))));
    }

    @Test
    void anOwnersRoomCannotBeEditedOrDeletedByAnotherOwner() {
        var otherHotel = mock(Hotel.class);
        when(otherHotel.getId()).thenReturn(20L);
        when(rooms.findById(5L)).thenReturn(Optional.of(new RoomType(otherHotel, "other", "Otro", "Descripción", 0)));
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.saveRoom(42L, 5L, room(3, 1, List.of()))).status);
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.deleteRoom(42L, 5L)).status);
        verify(rooms, never()).delete(any());
    }

    @Test
    void deletingADraftRoomRemovesItsDependentRecords() {
        var room = new RoomType(hotel, "suite", "Suite", "Descripción", 0);
        var photo = new RoomPhoto(room, "assets/images/suite.jpg", "Suite", 0);
        var detail = new RoomDetail(room, DetailKind.FEATURE, "WiFi", 0);
        var price = new Rate(room, RateMode.HOUR, BigDecimal.TEN, "PEN", null, null, false);
        room.getPhotos().add(photo);
        room.getDetails().add(detail);
        room.getRates().add(price);
        when(rooms.findById(5L)).thenReturn(Optional.of(room));
        service.deleteRoom(42L, 5L);
        verify(photos).delete(photo);
        verify(details).delete(detail);
        verify(rates).delete(price);
        verify(rooms).delete(room);
    }

    @Test
    void presentationReplacesCardsAndTrimsContent() {
        service.cards(42L, List.of(new StayCardInfo(" Descansa ", " Bienvenido ")));
        verify(cards).deleteByHotelId(10L);
        var saved = ArgumentCaptor.forClass(StayCard.class);
        verify(cards).save(saved.capture());
        assertEquals("Descansa", saved.getValue().getTitle());
        assertEquals("Bienvenido", saved.getValue().getDescription());
    }

    @Test
    void invalidPresentationIsRejectedBeforeDeletingExistingCards() {
        assertField("stayCards.0.title", () -> service.cards(42L, List.of(new StayCardInfo(" ", "Texto"))));
        verifyNoInteractions(cards);
    }

    @Test
    void photosFromAnotherRoomCannotBeAssigned() {
        var request = new RoomWrite("Suite", "Descripción", 0, 3, 1, null,
                List.of(new RoomPhotoWrite("assets/images/other.jpg")), List.of(), List.of());
        assertField("photos.0.path", () -> service.saveRoom(42L, null, request));
        verify(rooms, never()).save(any());
    }

    @Test
    void uploadedPhotoMustBelongToTheHotelAndRoomScope() {
        String scope = "room-" + UUID.randomUUID();
        var file = new MediaFile(10L, "image/png", "suite.png", 10);
        file.roomScope = scope;
        when(media.findById(file.id)).thenReturn(Optional.of(file));
        var request = new RoomWrite("Suite", "Descripción", 0, 3, 1, scope,
                List.of(new RoomPhotoWrite("/api/media/" + file.id)),
                List.of(new Detail(DetailKind.FEATURE, " WiFi ")), List.of());
        service.saveRoom(42L, null, request);
        var saved = ArgumentCaptor.forClass(RoomPhoto.class);
        verify(photos).save(saved.capture());
        assertEquals("Habitación Suite", saved.getValue().getAltText());
        file.roomScope = "another-room";
        assertField("photos.0.path", () -> service.saveRoom(42L, null, request));
    }

    @Test
    void photoScopesAcceptDraftUuidsOrAnExistingRoomOfThisHotel() {
        service.validatePhotoScope(hotel, "room-" + UUID.randomUUID());
        when(rooms.existsByHotelIdAndCode(10L, "suite")).thenReturn(true);
        service.validatePhotoScope(hotel, "suite");
        assertField("file", () -> service.validatePhotoScope(hotel, "other"));
        assertField("file", () -> service.validatePhotoScope(hotel, null));
    }

    @Test
    void mediaReferencesIncludeHotelIdentityAndRoomPhotos() {
        hotel.update("Hotel", "Texto", "Dirección", "Lima", null, "logo", "cover", "video", null, null, null, null, null);
        assertTrue(service.referenced(hotel, "logo"));
        assertTrue(service.referenced(hotel, "cover"));
        assertTrue(service.referenced(hotel, "video"));
        when(photos.existsByRoomTypeHotelIdAndPath(10L, "room-photo")).thenReturn(true);
        assertTrue(service.referenced(hotel, "room-photo"));
        assertFalse(service.referenced(hotel, "unused"));
    }

    @Test
    void requiredAndOversizedTextAreRejected() {
        assertField("name", () -> service.update(42L,
                new HotelWrite(" ", "Texto", "Dirección", "Lima", null, null, null, null, null, null, null, null, null, null, null, null)));
        assertField("name", () -> service.update(42L,
                new HotelWrite("x".repeat(201), "Texto", "Dirección", "Lima", null, null, null, null, null, null, null, null, null, null, null, null)));
    }
}
