package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.owner.MediaFile;
import java.util.UUID;
import java.util.List;
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByHotelIdAndRoomScopeIsNullOrderByOriginalNameAscIdAsc(Long hotelId);
}
