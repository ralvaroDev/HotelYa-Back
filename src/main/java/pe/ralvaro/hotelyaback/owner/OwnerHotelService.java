package pe.ralvaro.hotelyaback.owner;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.net.URI;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.application.*;
import pe.ralvaro.hotelyaback.domain.*;
import pe.ralvaro.hotelyaback.infrastructure.*;
import static pe.ralvaro.hotelyaback.application.CatalogDtos.*;
@Service @Transactional
public class OwnerHotelService {
    private final HotelRepository hotels; private final RoomTypeRepository rooms; private final CatalogService catalog;
    private final StayCardRepository stayCards;
    private final RoomPhotoRepository roomPhotos;
    private final RoomDetailRepository roomDetails;
    private final RateRepository roomRates;
    private final MediaFileRepository mediaFiles;
    public OwnerHotelService(HotelRepository hotels,RoomTypeRepository rooms,CatalogService catalog,
            StayCardRepository stayCards,RoomPhotoRepository roomPhotos,RoomDetailRepository roomDetails,
            RateRepository roomRates,MediaFileRepository mediaFiles) {
        this.hotels=hotels; this.rooms=rooms; this.catalog=catalog;
        this.stayCards=stayCards; this.roomPhotos=roomPhotos; this.roomDetails=roomDetails;
        this.roomRates=roomRates; this.mediaFiles=mediaFiles;
    }
    public record OwnerHotel(HotelDetail detail,boolean published) {}
    public record HotelWrite(String name,String description,String address,String location,String phone,String logoPath,String facadePath,String videoPath,
        BigDecimal latitude,BigDecimal longitude,String openingHours,String checkInOutConditions,Boolean hasParking,String mapUrl,LocalTime openingTime,LocalTime closingTime) {}
    public record RoomPhotoWrite(String path) {}
    public record RoomRateWrite(RateMode mode,BigDecimal amount,Integer packageHours,String currency) {}
    public record RoomWrite(String name,String description,Integer displayOrder,Integer totalUnits,Integer availableUnits,String photoScope,List<RoomPhotoWrite> photos,List<Detail> details,List<RoomRateWrite> rates) {}
    public Hotel owned(Long owner,boolean lock) {
        return (lock ? hotels.findByOwnerIdForUpdate(owner) : hotels.findByOwnerId(owner))
            .orElseThrow(() -> new ApiFailure(404,"Todavía no has creado tu hotel."));
    }
    @Transactional(readOnly=true) public OwnerHotel read(Long owner) { var h=owned(owner,false); return new OwnerHotel(catalog.ownerDetail(h.getId()),h.isPublished()); }
    public OwnerHotel create(Long owner,HotelWrite request) {
        if (hotels.findByOwnerId(owner).isPresent()) throw new ApiFailure(409,"Tu cuenta ya tiene un hotel.");
        var h=new Hotel("hotel-"+UUID.randomUUID(),"","","","",null,null,null,null); h.assignOwner(owner); h.setPublished(false);
        hotels.save(h); updateHotel(h,request); hotels.flush(); return read(owner);
    }
    public OwnerHotel update(Long owner,HotelWrite request) { var h=owned(owner,true); updateHotel(h,request); hotels.flush(); return read(owner); }
    private void updateHotel(Hotel h,HotelWrite r) {
        String name=text(r.name(),"name",200,true), description=text(r.description(),"description",10000,true), address=text(r.address(),"address",500,true), location=text(r.location(),"location",200,true);
        String phone=phone(r.phone(),"phone");
        String mapUrl=mapUrl(r.mapUrl());
        if ((r.openingTime()==null)!=(r.closingTime()==null)) throw ApiFailure.field(r.openingTime()==null ? "openingTime" : "closingTime","Completa las horas de apertura y cierre.");
        coordinate(r.latitude(),"latitude",90); coordinate(r.longitude(),"longitude",180);
        if ((r.latitude()==null)!=(r.longitude()==null)) throw ApiFailure.field("latitude","Completa ambas coordenadas o deja ambas vacías.");
        mediaPath(h,r.logoPath(),"logoPath",false); mediaPath(h,r.facadePath(),"facadePath",false); mediaPath(h,r.videoPath(),"videoPath",true);
        h.update(name,description,address,location,phone,r.logoPath(),r.facadePath(),r.videoPath(),r.latitude(),r.longitude(),
            text(r.openingHours(),"openingHours",5000,false),text(r.checkInOutConditions(),"checkInOutConditions",5000,false),r.hasParking());
        h.updateLocationAndHours(mapUrl,r.openingTime(),r.closingTime());
    }
    public OwnerHotel publication(Long owner,boolean publish) { var h=owned(owner,true); if(publish) validatePublication(h); h.setPublished(publish); hotels.flush(); return read(owner); }
    public OwnerHotel saveRoom(Long owner,Long id,RoomWrite r) {
        var h=owned(owner,true);
        boolean protectPublication=h.isPublished() && publicationReady(h);
        String scope=r.photoScope()==null ? "room-"+UUID.randomUUID() : r.photoScope();
        if(id==null && !scope.matches("room-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw ApiFailure.field("photos","Sube fotografías desde esta habitación.");
        RoomType type=id==null ? new RoomType(h,scope,"","",0) : room(h,id);
        if(id!=null && r.photoScope()!=null && !type.getCode().equals(r.photoScope())) throw ApiFailure.field("photos","Estas fotografías corresponden a otro tipo de habitación.");
        if(r.displayOrder()==null || r.displayOrder()<0) throw ApiFailure.field("displayOrder","El orden debe ser un entero no negativo.");
        if(r.totalUnits()==null || r.availableUnits()==null || r.totalUnits()<0 || r.availableUnits()<0 || r.availableUnits()>r.totalUnits()) throw ApiFailure.field("availableUnits","Completa cantidades enteras: disponibles entre cero y la cantidad total.");
        var photos=list(r.photos(),"photos",50); var details=list(r.details(),"details",100); var rates=list(r.rates(),"rates",3);
        Set<String> photoPaths=new HashSet<>();
        for(int i=0;i<photos.size();i++) {
            var p=photos.get(i); String field="photos."+i+".path";
            if(p==null || p.path()==null || !photoPaths.add(p.path())) throw ApiFailure.field(field,"Selecciona fotografías diferentes.");
            roomPhotoPath(h,type,p.path(),field);
        }
        for(int i=0;i<details.size();i++) { var d=details.get(i); if(d==null || d.kind()==null) throw ApiFailure.field("details."+i+".kind","Selecciona una categoría."); text(d.text(),"details."+i+".text",5000,true); }
        Set<RateMode> combinations=new HashSet<>();
        for(int i=0;i<rates.size();i++) {
            var rate=rates.get(i); String key="rates."+i;
            if(rate==null || rate.mode()==null) throw ApiFailure.field(key+".mode","Selecciona una modalidad.");
            if(rate.amount()==null || rate.amount().signum()<=0 || rate.amount().scale()>2 || rate.amount().precision()-rate.amount().scale()>10) throw ApiFailure.field(key+".amount","Introduce un importe positivo con hasta dos decimales.");
            if(rate.currency()!=null && !"PEN".equals(rate.currency())) throw ApiFailure.field(key+".currency","Las tarifas se manejan únicamente en soles.");
            if(rate.mode()==RateMode.PACKAGE) {
                if(rate.packageHours()==null || rate.packageHours()<=0) throw ApiFailure.field(key+".packageHours","Introduce una duración positiva en horas.");
            } else if(rate.packageHours()!=null) throw ApiFailure.field(key+".mode","La duración corresponde solo a paquetes.");
            if(!combinations.add(rate.mode())) throw ApiFailure.field(key+".mode","Solo se permite una tarifa por modalidad.");
        }
        type.update(text(r.name(),"name",200,true),text(r.description(),"description",10000,true),r.displayOrder(),r.totalUnits(),r.availableUnits());
        if(id==null) rooms.save(type);
        for(var p:new ArrayList<>(type.getPhotos())) roomPhotos.delete(p);
        for(var d:new ArrayList<>(type.getDetails())) roomDetails.delete(d);
        for(var rate:new ArrayList<>(type.getRates())) roomRates.delete(rate);
        type.getPhotos().clear(); type.getDetails().clear(); type.getRates().clear();
        for(int i=0;i<photos.size();i++) { var p=photos.get(i); var entity=new RoomPhoto(type,p.path(),"Habitación "+type.getName(),i); roomPhotos.save(entity); type.getPhotos().add(entity); }
        for(int i=0;i<details.size();i++) { var d=details.get(i); var entity=new RoomDetail(type,d.kind(),d.text().trim(),i); roomDetails.save(entity); type.getDetails().add(entity); }
        for(var rate:rates) { var entity=new Rate(type,rate.mode(),rate.amount(),"PEN",rate.packageHours(),null,false); roomRates.save(entity); type.getRates().add(entity); }
        hotels.flush(); if(protectPublication) validatePublication(h); return read(owner);
    }
    public OwnerHotel deleteRoom(Long owner,Long id) {
        var h=owned(owner,true); boolean protectPublication=h.isPublished() && publicationReady(h); var type=room(h,id);
        type.getPhotos().forEach(roomPhotos::delete); type.getDetails().forEach(roomDetails::delete); type.getRates().forEach(roomRates::delete);
        rooms.delete(type); hotels.flush(); if(protectPublication) validatePublication(h); return read(owner);
    }
    private RoomType room(Hotel h,Long id) { var r=rooms.findById(id).orElse(null); if(r==null || !r.getHotel().getId().equals(h.getId())) throw new ApiFailure(404,"Tipo de habitación no encontrado."); return r; }
    public OwnerHotel cards(Long owner,List<StayCardInfo> request) {
        var h=owned(owner,true); var cards=list(request,"stayCards",20);
        for(int i=0;i<cards.size();i++) { if(cards.get(i)==null) throw ApiFailure.field("stayCards."+i,"Completa la tarjeta."); text(cards.get(i).title(),"stayCards."+i+".title",200,true); text(cards.get(i).description(),"stayCards."+i+".description",5000,true); }
        stayCards.deleteByHotelId(h.getId());
        for(int i=0;i<cards.size();i++) stayCards.save(new StayCard(h.getId(),cards.get(i).title().trim(),cards.get(i).description().trim(),i));
        hotels.flush(); return read(owner);
    }
    public boolean referenced(Hotel h,String path) {
        if(path.equals(h.getLogoPath()) || path.equals(h.getFacadePath()) || path.equals(h.getVideoPath())) return true;
        return roomPhotos.existsByRoomTypeHotelIdAndPath(h.getId(),path);
    }
    public void validatePhotoScope(Hotel h,String scope) {
        if(scope!=null && scope.matches("room-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) return;
        if(scope!=null && scope.length()<=80 && rooms.existsByHotelIdAndCode(h.getId(),scope)) return;
        throw ApiFailure.field("file","Sube la imagen desde el tipo de habitación correspondiente.");
    }
    private void roomPhotoPath(Hotel h,RoomType room,String path,String field) {
        if(room.getPhotos().stream().anyMatch(p -> p.getPath().equals(path))) return;
        if(path.startsWith("/api/media/")) {
            try {
                var media=mediaFiles.findById(UUID.fromString(path.substring(11))).orElse(null);
                if(media!=null && media.hotelId.equals(h.getId()) && room.getCode().equals(media.roomScope) && media.contentType.startsWith("image/") && path.equals("/api/media/"+media.id)) return;
            } catch(IllegalArgumentException ignored) {}
        }
        throw ApiFailure.field(field,"Sube una imagen nueva para este tipo de habitación. Las fotografías no se comparten.");
    }
    private void mediaPath(Hotel h,String path,String field,boolean video) {
        if(path==null) return;
        if(path.startsWith("/api/media/")) {
            try {
                var media=mediaFiles.findById(UUID.fromString(path.substring(11))).orElse(null);
                if(media!=null && media.hotelId.equals(h.getId()) && media.roomScope==null && path.equals("/api/media/"+media.id) && media.contentType.startsWith(video?"video/":"image/")) return;
            } catch(IllegalArgumentException ignored) {}
        } else if(path.startsWith(video?"assets/videos/":"assets/images/") && referenced(h,path)) return;
        throw ApiFailure.field(field,"Selecciona un archivo de tu hotel.");
    }
    private boolean publicationReady(Hotel h) {
        return rooms.findByHotelIdOrderByDisplayOrderAscIdAsc(h.getId()).stream().anyMatch(r ->
            r.getTotalUnits()!=null && r.getAvailableUnits()!=null && !r.getPhotos().isEmpty() && !r.getRates().isEmpty());
    }
    private void validatePublication(Hotel h) {
        if(!publicationReady(h)) throw new ApiFailure(409,"En Habitaciones, completa la cantidad total y las disponibles, una tarifa y una foto de al menos un tipo. Si quieres retirar el último tipo completo, oculta el hotel desde Inicio.");
    }
    private static String mapUrl(String value) {
        String result=text(value,"mapUrl",2048,false);
        if(result==null) return null;
        try {
            URI uri=URI.create(result);
            if("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null) return result;
        } catch(IllegalArgumentException ignored) {}
        throw ApiFailure.field("mapUrl","Pega una URL HTTPS válida del lugar.");
    }
    public static String text(String value,String field,int max,boolean required) {
        if(value==null || value.trim().isEmpty()) { if(required) throw ApiFailure.field(field,"Este campo es obligatorio."); return null; }
        String result=value.trim(); if(result.length()>max) throw ApiFailure.field(field,"Máximo "+max+" caracteres."); return result;
    }
    private static String phone(String value,String field) { String result=text(value,field,40,false); if(result!=null && !result.matches("\\+51[0-9]{9}")) throw ApiFailure.field(field,"Usa +51 seguido de exactamente 9 dígitos, sin letras ni espacios."); return result; }
    private static void coordinate(BigDecimal value,String field,int bound) { if(value!=null && (value.abs().compareTo(BigDecimal.valueOf(bound))>0 || value.scale()>6)) throw ApiFailure.field(field,"Coordenada fuera de rango o con más de seis decimales."); }
    private static <T> List<T> list(List<T> value,String field,int max) { if(value==null) return List.of(); if(value.size()>max) throw ApiFailure.field(field,"Máximo "+max+" elementos."); return value; }
}
