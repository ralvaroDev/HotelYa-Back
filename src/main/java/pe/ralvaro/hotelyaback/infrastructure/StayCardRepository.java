package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.StayCard;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface StayCardRepository extends JpaRepository<StayCard, Long> {
    List<StayCard> findByHotelIdOrderByDisplayOrderAscIdAsc(Long hotelId);

    @Modifying
    @Query("delete from StayCard c where c.hotelId = :hotelId")
    void deleteByHotelId(@Param("hotelId") Long hotelId);
}
