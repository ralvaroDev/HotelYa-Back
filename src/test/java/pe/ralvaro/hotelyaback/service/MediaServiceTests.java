package pe.ralvaro.hotelyaback.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import pe.ralvaro.hotelyaback.exception.ApiFailure;
import pe.ralvaro.hotelyaback.model.Hotel;
import pe.ralvaro.hotelyaback.model.MediaFile;
import pe.ralvaro.hotelyaback.repository.HotelRepository;
import pe.ralvaro.hotelyaback.repository.MediaFileRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaServiceTests {
    @TempDir
    Path directory;
    private final OwnerHotelService owners = mock(OwnerHotelService.class);
    private final MediaFileRepository files = mock(MediaFileRepository.class);
    private final HotelRepository hotels = mock(HotelRepository.class);
    private final Hotel hotel = mock(Hotel.class);
    private MediaService service;

    @BeforeEach
    void setup() {
        service = new MediaService(owners, files, hotels, directory.toString());
        when(hotel.getId()).thenReturn(10L);
        when(owners.owned(42L, false)).thenReturn(hotel);
        when(owners.owned(42L, true)).thenReturn(hotel);
    }

    private MediaFile file(Long hotelId) throws Exception {
        var file = new MediaFile(hotelId, "image/png", "foto.png", 3);
        Files.write(service.path(file.id), new byte[]{1, 2, 3});
        when(files.findById(file.id)).thenReturn(Optional.of(file));
        return file;
    }

    @Test
    void libraryUsesOnlyTheOwnersHotelAndExcludesRoomScopedFiles() {
        var file = new MediaFile(10L, "image/png", "foto.png", 3);
        when(files.findByHotelIdAndRoomScopeIsNullOrderByOriginalNameAscIdAsc(10L)).thenReturn(List.of(file));
        var result = service.list(42L);
        assertEquals(1, result.size());
        assertEquals("/api/media/" + file.id, result.getFirst().path());
        assertEquals("foto.png", result.getFirst().originalName());
    }

    @Test
    void emptyAndFakeUploadsAreRejectedBeforePersistence() {
        var empty = new MockMultipartFile("file", new byte[0]);
        var fake = new MockMultipartFile("file", "foto.png", "image/png", new byte[]{1, 2, 3});
        assertEquals(400, assertThrows(ApiFailure.class, () -> service.upload(42L, empty, null)).status);
        assertEquals(400, assertThrows(ApiFailure.class, () -> service.upload(42L, fake, null)).status);
        verifyNoInteractions(files);
    }

    @Test
    void filesOfAnotherHotelCannotBeReadOrDeleted() throws Exception {
        var file = file(99L);
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.privateFile(42L, file.id)).status);
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.delete(42L, file.id)).status);
        verify(files, never()).delete(any());
        assertTrue(Files.exists(service.path(file.id)));
    }

    @Test
    void ownerCanReadAnExistingFileOfTheirHotel() throws Exception {
        var file = file(10L);
        assertSame(file, service.privateFile(42L, file.id));
    }

    @Test
    void missingRecordOrPhysicalFileReturns404() throws Exception {
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.privateFile(42L, UUID.randomUUID())).status);
        var file = file(10L);
        Files.delete(service.path(file.id));
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.privateFile(42L, file.id)).status);
    }

    @Test
    void onlyReferencedFilesOfPublishedHotelsArePublic() throws Exception {
        var file = file(10L);
        when(hotels.findById(10L)).thenReturn(Optional.of(hotel));
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.publicFile(file.id)).status);
        when(hotel.isPublished()).thenReturn(true);
        assertEquals(404, assertThrows(ApiFailure.class, () -> service.publicFile(file.id)).status);
        when(owners.referenced(hotel, "/api/media/" + file.id)).thenReturn(true);
        assertSame(file, service.publicFile(file.id));
    }

    @Test
    void referencedFilesCannotBeDeleted() throws Exception {
        var file = file(10L);
        when(owners.referenced(hotel, "/api/media/" + file.id)).thenReturn(true);
        assertEquals(409, assertThrows(ApiFailure.class, () -> service.delete(42L, file.id)).status);
        verify(files, never()).delete(any());
        assertTrue(Files.exists(service.path(file.id)));
    }
}
