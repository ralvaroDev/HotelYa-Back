package pe.ralvaro.hotelyaback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.ralvaro.hotelyaback.model.Rate;

public interface RateRepository extends JpaRepository<Rate, Long> {

}
