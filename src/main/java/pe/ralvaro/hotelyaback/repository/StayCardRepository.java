package pe.ralvaro.hotelyaback.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ralvaro.hotelyaback.model.StayCard;

public interface StayCardRepository extends JpaRepository<StayCard, Long> {
    List<StayCard> findByHotelIdOrderByDisplayOrderAscIdAsc(Long hotelId);

    @Modifying
    @Query("delete from StayCard c where c.hotelId = :hotelId")
    void deleteByHotelId(@Param("hotelId") Long hotelId);
}
