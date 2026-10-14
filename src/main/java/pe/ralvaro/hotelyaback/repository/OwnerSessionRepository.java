package pe.ralvaro.hotelyaback.repository;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ralvaro.hotelyaback.model.OwnerSession;

public interface OwnerSessionRepository extends JpaRepository<OwnerSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OwnerSession s where s.id = :id")
    Optional<OwnerSession> findByIdForUpdate(@Param("id") UUID id);
}
