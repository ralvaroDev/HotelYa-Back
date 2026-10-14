package pe.ralvaro.hotelyaback.owner;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.application.*;
import pe.ralvaro.hotelyaback.domain.*;
import pe.ralvaro.hotelyaback.infrastructure.*;
import static pe.ralvaro.hotelyaback.application.CatalogDtos.*;
@Service @Transactional
public class OwnerHotelService {
    @PersistenceContext private EntityManager entities;
    private final HotelRepository hotels; private final RoomTypeRepository rooms; private final CatalogService catalog;
    public OwnerHotelService(HotelRepository hotels,RoomTypeRepository rooms,CatalogService catalog) { this.hotels=hotels; this.rooms=rooms; this.catalog=catalog; }
    public record OwnerHotel(HotelDetail detail,boolean published) {}
    public record HotelWrite(String name,String description,String address,String location,String phone,String logoPath,String facadePath,String videoPath,
        BigDecimal latitude,BigDecimal longitude,String whatsapp,String openingHours,String checkInOutConditions,Boolean hasParking) {}
    public record RoomWrite(String name,String description,Integer displayOrder,Integer totalUnits,Integer availableUnits,List<Photo> photos,List<Detail> details,List<RateInfo> rates) {}
    public Hotel owned(Long owner,boolean lock) {
        var hotel=hotels.findByOwnerId(owner).orElseThrow(() -> new ApiFailure(404,"Todavía no has creado tu hotel."));
        if (lock) entities.lock(hotel,LockModeType.PESSIMISTIC_WRITE); return hotel;
    }
    @Transactional(readOnly=true) public OwnerHotel read(Long owner) { var h=owned(owner,false); return new OwnerHotel(catalog.ownerDetail(h.getId()),h.isPublished()); }
    public OwnerHotel create(Long owner,HotelWrite request) {
        if (hotels.findByOwnerId(owner).isPresent()) throw new ApiFailure(409,"Tu cuenta ya tiene un hotel.");
        var h=new Hotel("hotel-"+UUID.randomUUID(),"","","","",null,null,null,null); h.assignOwner(owner); h.setPublished(false);
        entities.persist(h); updateHotel(h,request); entities.flush(); return read(owner);
    }
    public OwnerHotel update(Long owner,HotelWrite request) { var h=owned(owner,true); updateHotel(h,request); enforcePublished(h); entities.flush(); return read(owner); }
    private void updateHotel(Hotel h,HotelWrite r) {
        String name=text(r.name(),"name",200,true), description=text(r.description(),"description",10000,true), address=text(r.address(),"address",500,true), location=text(r.location(),"location",200,true);
        String phone=phone(r.phone(),"phone"), whatsapp=phone(r.whatsapp(),"whatsapp");
        coordinate(r.latitude(),"latitude",90); coordinate(r.longitude(),"longitude",180);
        if ((r.latitude()==null)!=(r.longitude()==null)) throw ApiFailure.field("latitude","Completa ambas coordenadas o deja ambas vacías.");
        mediaPath(h,r.logoPath(),"logoPath",false); mediaPath(h,r.facadePath(),"facadePath",false); mediaPath(h,r.videoPath(),"videoPath",true);
        h.update(name,description,address,location,phone,r.logoPath(),r.facadePath(),r.videoPath(),r.latitude(),r.longitude(),whatsapp,
            text(r.openingHours(),"openingHours",5000,false),text(r.checkInOutConditions(),"checkInOutConditions",5000,false),r.hasParking());
    }
    public OwnerHotel publication(Long owner,boolean publish) { var h=owned(owner,true); if(publish) validatePublication(h); h.setPublished(publish); entities.flush(); return read(owner); }
    private RoomType room(Hotel h,Long id) { var r=entities.find(RoomType.class,id); if(r==null || !r.getHotel().getId().equals(h.getId())) throw new ApiFailure(404,"Tipo de habitación no encontrado."); return r; }
    public boolean referenced(Hotel h,String path) {
        if(path.equals(h.getLogoPath()) || path.equals(h.getFacadePath()) || path.equals(h.getVideoPath())) return true;
        return entities.createQuery("select count(p) from RoomPhoto p where p.roomType.hotel.id=:hotel and p.path=:path",Long.class).setParameter("hotel",h.getId()).setParameter("path",path).getSingleResult()>0;
    }
    private void mediaPath(Hotel h,String path,String field,boolean video) {
        if(path==null) return;
        if(path.startsWith("/api/media/")) {
            try {
                var media=entities.find(MediaFile.class,UUID.fromString(path.substring(11)));
                if(media!=null && media.hotelId.equals(h.getId()) && path.equals("/api/media/"+media.id) && media.contentType.startsWith(video?"video/":"image/")) return;
            } catch(IllegalArgumentException ignored) {}
        } else if(path.startsWith(video?"assets/videos/":"assets/images/") && referenced(h,path)) return;
        throw ApiFailure.field(field,"Selecciona un archivo de tu hotel.");
    }
    private void enforcePublished(Hotel h) { if(h.isPublished()) validatePublication(h); }
    private void validatePublication(Hotel h) {
        var types=rooms.findByHotelIdOrderByDisplayOrderAscIdAsc(h.getId());
        boolean ready=types.stream().anyMatch(r -> r.getTotalUnits()!=null && r.getAvailableUnits()!=null && !r.getPhotos().isEmpty() && !r.getRates().isEmpty());
        if(!ready) throw new ApiFailure(409,"Para publicar o editar un hotel publicado necesitas al menos un tipo con cantidades, una tarifa y una foto. Oculta el hotel para completar su configuración.");
    }
    public static String text(String value,String field,int max,boolean required) {
        if(value==null || value.trim().isEmpty()) { if(required) throw ApiFailure.field(field,"Este campo es obligatorio."); return null; }
        String result=value.trim(); if(result.length()>max) throw ApiFailure.field(field,"Máximo "+max+" caracteres."); return result;
    }
    private static String phone(String value,String field) { String result=text(value,field,40,false); if(result!=null && !result.matches("\\+[0-9]{7,15}")) throw ApiFailure.field(field,"Usa formato internacional, por ejemplo +51987654321."); return result; }
    private static void coordinate(BigDecimal value,String field,int bound) { if(value!=null && (value.abs().compareTo(BigDecimal.valueOf(bound))>0 || value.scale()>6)) throw ApiFailure.field(field,"Coordenada fuera de rango o con más de seis decimales."); }
    private static <T> List<T> list(List<T> value,String field,int max) { if(value==null) return List.of(); if(value.size()>max) throw ApiFailure.field(field,"Máximo "+max+" elementos."); return value; }
}
