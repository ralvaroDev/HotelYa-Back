package pe.ralvaro.hotelyaback.dto.request;

import java.util.List;

import pe.ralvaro.hotelyaback.dto.catalog.CatalogDtos.Detail;

public record RoomWrite(String name, String description, Integer displayOrder, Integer totalUnits,
                        Integer availableUnits, String photoScope, List<RoomPhotoWrite> photos, List<Detail> details,
                        List<RoomRateWrite> rates) {
}
