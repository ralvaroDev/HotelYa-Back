package pe.ralvaro.hotelyaback.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="media_file")
public class MediaFile {
    @Id public UUID id;
    @Column(name="hotel_id",nullable=false) public Long hotelId;
    @Column(name="content_type",nullable=false,length=50) public String contentType;
    @Column(name="original_name",nullable=false,length=255) public String originalName;
    @Column(name="byte_size",nullable=false) public long byteSize;
    @Column(name="room_scope",length=80) public String roomScope;
    protected MediaFile() {}
    public MediaFile(Long hotelId,String type,String name,long size) {
        id=UUID.randomUUID(); this.hotelId=hotelId; contentType=type; originalName=name; byteSize=size;
    }
}
