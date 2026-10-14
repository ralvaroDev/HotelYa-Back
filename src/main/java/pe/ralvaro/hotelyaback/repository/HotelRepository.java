package pe.ralvaro.hotelyaback.repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ralvaro.hotelyaback.model.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Optional<Hotel> findByOwnerId(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.ownerId = :ownerId")
    Optional<Hotel> findByOwnerIdForUpdate(@Param("ownerId") Long ownerId);

    Optional<Hotel> findByCode(String code);
}
