package pe.ralvaro.hotelyaback.dto.request;

import java.math.BigDecimal;
import pe.ralvaro.hotelyaback.model.RateMode;

public record RoomRateWrite(RateMode mode,BigDecimal amount,Integer packageHours,String currency) {}
