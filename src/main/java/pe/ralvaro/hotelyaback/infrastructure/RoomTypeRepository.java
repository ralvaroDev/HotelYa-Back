package pe.ralvaro.hotelyaback.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelIdOrderByDisplayOrderAscIdAsc(Long hotelId);
    boolean existsByHotelIdAndCode(Long hotelId, String code);
}
