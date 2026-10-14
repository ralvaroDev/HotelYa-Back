package pe.ralvaro.hotelyaback.dto.response;

import pe.ralvaro.hotelyaback.dto.catalog.CatalogDtos.HotelDetail;

public record OwnerHotelResponse(HotelDetail detail,boolean published) {}
