package pe.ralvaro.hotelyaback.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.domain.Rate;

public interface RateRepository extends JpaRepository<Rate, Long> {

}
