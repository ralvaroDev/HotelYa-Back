package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.RoomDetail;

public interface RoomDetailRepository extends JpaRepository<RoomDetail, Long> {

}
