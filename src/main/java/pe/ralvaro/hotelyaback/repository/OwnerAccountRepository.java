package pe.ralvaro.hotelyaback.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.model.OwnerAccount;

public interface OwnerAccountRepository extends JpaRepository<OwnerAccount, Long> {
    Optional<OwnerAccount> findByEmail(String email);
}
