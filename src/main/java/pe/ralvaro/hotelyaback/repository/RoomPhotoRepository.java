package pe.ralvaro.hotelyaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.model.RoomPhoto;

public interface RoomPhotoRepository extends JpaRepository<RoomPhoto, Long> {
    boolean existsByRoomTypeHotelIdAndPath(Long hotelId, String path);
}
