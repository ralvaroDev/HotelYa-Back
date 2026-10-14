package pe.ralvaro.hotelyaback.owner;
import jakarta.persistence.*;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import pe.ralvaro.hotelyaback.domain.Hotel;
@Service @Transactional
public class MediaService {
    @PersistenceContext private EntityManager entities;
    private final OwnerHotelService owners; private final Path directory;
    public MediaService(OwnerHotelService owners,@Value("${owner.upload-dir}") String directory) {
        this.owners=owners; this.directory=Path.of(directory).toAbsolutePath().normalize();
        try { Files.createDirectories(this.directory); } catch(IOException e) { throw new IllegalStateException("No se pudo preparar HOTELYA_UPLOAD_DIR.",e); }
    }
    public record MediaInfo(UUID id,String path,String contentType,String originalName,long byteSize) {}
    private MediaInfo info(MediaFile file) { return new MediaInfo(file.id,"/api/media/"+file.id,file.contentType,file.originalName,file.byteSize); }
    @Transactional(readOnly=true) public List<MediaInfo> list(Long owner) {
        var h=owners.owned(owner,false); return entities.createQuery("from MediaFile where hotelId=:hotel and roomScope is null order by originalName,id",MediaFile.class).setParameter("hotel",h.getId()).getResultStream().map(this::info).toList();
    }
    public MediaInfo upload(Long owner,MultipartFile file,String roomScope) throws IOException {
        var hotel=owners.owned(owner,true);
        if(roomScope!=null) owners.validatePhotoScope(hotel,roomScope);
        if(file.isEmpty() || file.getSize()>100L*1024*1024) throw ApiFailure.field("file","Selecciona un archivo de hasta 100 MB.");
        byte[] bytes=file.getBytes();
        boolean image=bytes.length>24 && ((bytes[0]==(byte)137 && ascii(bytes,1,3).equals("PNG"))
            || (bytes[0]==(byte)255 && bytes[1]==(byte)216 && bytes[2]==(byte)255)
            || (ascii(bytes,0,4).equals("RIFF") && ascii(bytes,8,4).equals("WEBP")));
        if(image && bytes.length>10*1024*1024) throw ApiFailure.field("file","La imagen supera los 10 MB.");
        String type=detect(bytes);
        if(type==null) throw ApiFailure.field("file","El contenido debe ser JPEG, PNG, WebP o MP4 válido.");
        if(type.startsWith("image/") && bytes.length>10*1024*1024) throw ApiFailure.field("file","La imagen supera los 10 MB.");
        if(roomScope!=null && !type.startsWith("image/")) throw ApiFailure.field("file","Sube una imagen JPEG, PNG o WebP para la habitación.");
        String name=file.getOriginalFilename()==null ? "Archivo" : file.getOriginalFilename().replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        name=name.isBlank()?"Archivo":name.substring(0,Math.min(name.length(),255));
        var media=new MediaFile(hotel.getId(),type,name,bytes.length); media.roomScope=roomScope; Path target=directory.resolve(media.id.toString());
        Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) removeFile(target); }
        });
        entities.persist(media); return info(media);
    }
    public void delete(Long owner,UUID id) {
        var hotel=owners.owned(owner,true); var file=find(id);
        if(!file.hotelId.equals(hotel.getId())) throw new ApiFailure(404,"Archivo no encontrado.");
        if(owners.referenced(hotel,"/api/media/"+id)) throw new ApiFailure(409,"Quita el archivo de la ficha o de las habitaciones y guarda los cambios antes de eliminarlo.");
        entities.remove(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() { @Override public void afterCommit() { removeFile(directory.resolve(id.toString())); } });
    }
    private void removeFile(Path path) { try { Files.deleteIfExists(path); } catch(IOException e) { org.slf4j.LoggerFactory.getLogger(MediaService.class).warn("No se pudo eliminar el archivo {}",path.getFileName()); } }
    @Transactional(readOnly=true) public MediaFile publicFile(UUID id) {
        var file=find(id); var h=entities.find(Hotel.class,file.hotelId);
        if(!h.isPublished() || !owners.referenced(h,"/api/media/"+id)) throw new ApiFailure(404,"Archivo no encontrado."); return file;
    }
    @Transactional(readOnly=true) public MediaFile privateFile(Long owner,UUID id) { var h=owners.owned(owner,false); var f=find(id); if(!h.getId().equals(f.hotelId)) throw new ApiFailure(404,"Archivo no encontrado."); return f; }
    private MediaFile find(UUID id) { var file=entities.find(MediaFile.class,id); if(file==null || !Files.isRegularFile(directory.resolve(id.toString()))) throw new ApiFailure(404,"Archivo no encontrado."); return file; }
    public Path path(UUID id) { return directory.resolve(id.toString()); }
    static String detect(byte[] b) {
        try {
            boolean png=b.length>24 && b[0]==(byte)137 && ascii(b,1,3).equals("PNG");
            boolean jpeg=b.length>4 && b[0]==(byte)255 && b[1]==(byte)216 && b[2]==(byte)255;
            boolean webp=b.length>24 && ascii(b,0,4).equals("RIFF") && ascii(b,8,4).equals("WEBP");
            if(png || jpeg || webp) {
                // Read dimensions before decoding to reject decompression bombs.
                try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
                    var readers=ImageIO.getImageReaders(input); if(!readers.hasNext()) return null;
                    var reader=readers.next(); try { reader.setInput(input); if((long)reader.getWidth(0)*reader.getHeight(0)>40_000_000) return null; if(reader.read(0)==null) return null; } finally { reader.dispose(); }
                }
                return png?"image/png":jpeg?"image/jpeg":"image/webp";
            }
            int pos=0; boolean ftyp=false,moov=false,mdat=false,video=false;
            while(pos+8<=b.length) {
                long size=Integer.toUnsignedLong(ByteBuffer.wrap(b,pos,4).getInt()); int header=8;
                if(size==1) { if(pos+16>b.length) return null; size=ByteBuffer.wrap(b,pos+8,8).getLong(); header=16; }
                if(size==0) size=b.length-pos;
                if(size<header || size>b.length-pos) return null;
                String box=ascii(b,pos+4,4);
                if(box.equals("ftyp") && size>=16) ftyp=true;
                if(box.equals("moov")) { moov=true; video=hasVideoTrack(b,pos+header,(int)(pos+size),0); }
                if(box.equals("mdat") && size>header) mdat=true;
                pos+=(int)size;
            }
            return ftyp && moov && mdat && video && pos==b.length?"video/mp4":null;
        } catch(IOException | RuntimeException e) { return null; }
    }
    private static boolean hasVideoTrack(byte[] bytes,int start,int end,int depth) {
        if(depth>8) return false; boolean video=false;
        for(int pos=start;pos+8<=end;) {
            long size=Integer.toUnsignedLong(ByteBuffer.wrap(bytes,pos,4).getInt()); if(size<8 || size>end-pos) return false;
            String kind=ascii(bytes,pos+4,4);
            if(kind.equals("hdlr") && size>=20 && ascii(bytes,pos+16,4).equals("vide")) video=true;
            if(Set.of("trak","mdia").contains(kind)) video |= hasVideoTrack(bytes,pos+8,(int)(pos+size),depth+1);
            pos+=(int)size;
        }
        return video;
    }
    private static String ascii(byte[] bytes,int offset,int length) { return new String(bytes,offset,length,StandardCharsets.US_ASCII); }
}
