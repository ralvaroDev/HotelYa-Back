package pe.ralvaro.hotelyaback.owner;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static pe.ralvaro.hotelyaback.owner.OwnerHotelService.*;
import pe.ralvaro.hotelyaback.application.CatalogDtos.StayCardInfo;
@RestController @RequestMapping("/api/owner/hotel")
public class OwnerHotelController {
    private final OwnerHotelService service;
    public OwnerHotelController(OwnerHotelService service) { this.service=service; }
    private Long owner(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
    @GetMapping public OwnerHotel get(@AuthenticationPrincipal Jwt jwt) { return service.read(owner(jwt)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public OwnerHotel create(@AuthenticationPrincipal Jwt jwt,@RequestBody HotelWrite request) { return service.create(owner(jwt),request); }
    @PutMapping public OwnerHotel update(@AuthenticationPrincipal Jwt jwt,@RequestBody HotelWrite request) { return service.update(owner(jwt),request); }
    @PutMapping("/publication") public OwnerHotel publication(@AuthenticationPrincipal Jwt jwt,@RequestBody Publication request) { return service.publication(owner(jwt),request.published()); }
    public record Publication(boolean published) {}
    @PostMapping("/room-types") @ResponseStatus(HttpStatus.CREATED) public OwnerHotel add(@AuthenticationPrincipal Jwt jwt,@RequestBody RoomWrite request) { return service.saveRoom(owner(jwt),null,request); }
    @PutMapping("/room-types/{id}") public OwnerHotel updateRoom(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id,@RequestBody RoomWrite request) { return service.saveRoom(owner(jwt),id,request); }
    @DeleteMapping("/room-types/{id}") public OwnerHotel deleteRoom(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id) { return service.deleteRoom(owner(jwt),id); }
}
