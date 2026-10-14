package pe.ralvaro.hotelyaback.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_detail")
public class RoomDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 40, nullable = false)
    private DetailKind kind;
    @Column(name = "text", columnDefinition = "text", nullable = false)
    private String text;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected RoomDetail() {}

    public RoomDetail(RoomType roomType, DetailKind kind, String text, int displayOrder) {
        this.roomType = roomType;
        this.kind = kind;
        this.text = text;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public RoomType getRoomType() { return roomType; }
    public DetailKind getKind() { return kind; }
    public String getText() { return text; }
    public int getDisplayOrder() { return displayOrder; }
}
