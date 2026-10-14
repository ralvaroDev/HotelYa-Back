package pe.ralvaro.hotelyaback.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.model.MediaFile;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByHotelIdAndRoomScopeIsNullOrderByOriginalNameAscIdAsc(Long hotelId);
}
