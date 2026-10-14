package pe.ralvaro.hotelyaback.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.ralvaro.hotelyaback.dto.catalog.CatalogDtos.StayCardInfo;
import pe.ralvaro.hotelyaback.dto.request.HotelWrite;
import pe.ralvaro.hotelyaback.dto.request.PublicationRequest;
import pe.ralvaro.hotelyaback.dto.request.RoomWrite;
import pe.ralvaro.hotelyaback.dto.response.OwnerHotelResponse;
import pe.ralvaro.hotelyaback.service.OwnerHotelService;

@RestController @RequestMapping("/api/owner/hotel")
public class OwnerHotelController {
    private final OwnerHotelService service;
    public OwnerHotelController(OwnerHotelService service) { this.service=service; }
    private Long owner(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
    @GetMapping public OwnerHotelResponse get(@AuthenticationPrincipal Jwt jwt) { return service.read(owner(jwt)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OwnerHotelResponse create(@AuthenticationPrincipal Jwt jwt,@RequestBody HotelWrite request) { return service.create(owner(jwt),request); }
    @PutMapping public OwnerHotelResponse update(@AuthenticationPrincipal Jwt jwt,@RequestBody HotelWrite request) { return service.update(owner(jwt),request); }
    @PutMapping("/publication") public OwnerHotelResponse publication(@AuthenticationPrincipal Jwt jwt,@RequestBody PublicationRequest request) { return service.publication(owner(jwt),request.published()); }

    @PostMapping("/room-types") @ResponseStatus(HttpStatus.CREATED) public OwnerHotelResponse add(@AuthenticationPrincipal Jwt jwt,@RequestBody RoomWrite request) { return service.saveRoom(owner(jwt),null,request); }
    @PutMapping("/room-types/{id}") public OwnerHotelResponse updateRoom(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody RoomWrite request) { return service.saveRoom(owner(jwt),id,request); }
    @DeleteMapping("/room-types/{id}") public OwnerHotelResponse deleteRoom(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) { return service.deleteRoom(owner(jwt),id); }
    @PutMapping("/stay-cards") public OwnerHotelResponse cards(@AuthenticationPrincipal Jwt jwt,@RequestBody List<StayCardInfo> request) { return service.cards(owner(jwt),request); }
}
