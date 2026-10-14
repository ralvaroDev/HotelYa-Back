package pe.ralvaro.hotelyaback.dto.request;

import java.math.BigDecimal;
import java.time.LocalTime;

public record HotelWrite(String name, String description, String address, String location, String phone,
                         String logoPath, String facadePath, String videoPath,
                         BigDecimal latitude, BigDecimal longitude, String openingHours, String checkInOutConditions,
                         Boolean hasParking, String mapUrl, LocalTime openingTime, LocalTime closingTime) {
}
