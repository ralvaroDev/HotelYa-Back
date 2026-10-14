package pe.ralvaro.hotelyaback.application;

import java.math.BigDecimal;
import java.util.List;
import pe.ralvaro.hotelyaback.domain.DetailKind;
import pe.ralvaro.hotelyaback.domain.RateMode;

public final class CatalogDtos {
    private CatalogDtos() {}
    public record HotelSummary(Long id, String name, String description, String location, String logoPath) {}
    public record HotelDetail(Long id, String name, String description, String address, String location,
            String phone, String logoPath, String facadePath, String videoPath, BigDecimal latitude,
            BigDecimal longitude, String whatsapp, String openingHours, String checkInOutConditions,
            Boolean hasParking, List<RoomTypeDetail> roomTypes) {}
    public record RoomTypeDetail(Long id, String code, String name, String description,
            List<Photo> photos, List<Detail> details, List<RateInfo> rates) {}
    public record Photo(String path, String altText) {}
    public record Detail(DetailKind kind, String text) {}
    public record RateInfo(RateMode mode, BigDecimal amount, String currency, Integer packageHours,
            BigDecimal additionalHourAmount, boolean simulated) {}
}
