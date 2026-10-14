package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.RoomPhoto;

public interface RoomPhotoRepository extends JpaRepository<RoomPhoto, Long> {
    boolean existsByRoomTypeHotelIdAndPath(Long hotelId, String path);
}
