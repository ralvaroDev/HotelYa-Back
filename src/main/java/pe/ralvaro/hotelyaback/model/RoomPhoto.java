package pe.ralvaro.hotelyaback.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_photo")
public class RoomPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
    @Column(name = "path", length = 500, nullable = false)
    private String path;
    @Column(name = "alt_text", length = 300, nullable = false)
    private String altText;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected RoomPhoto() {
    }

    public RoomPhoto(RoomType roomType, String path, String altText, int displayOrder) {
        this.roomType = roomType;
        this.path = path;
        this.altText = altText;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getPath() {
        return path;
    }

    public String getAltText() {
        return altText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
