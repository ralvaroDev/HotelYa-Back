package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.OwnerAccount;
import java.util.Optional;
public interface OwnerAccountRepository extends JpaRepository<OwnerAccount, Long> {
    Optional<OwnerAccount> findByEmail(String email);
}
