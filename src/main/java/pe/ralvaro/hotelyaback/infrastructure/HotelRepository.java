package pe.ralvaro.hotelyaback.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Optional<Hotel> findByOwnerId(Long ownerId);
    Optional<Hotel> findByCode(String code);
}
