package pe.ralvaro.hotelyaback.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "room_type", uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "code"}))
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;
    @Column(name = "code", length = 80, nullable = false)
    private String code;
    @Column(name = "name", length = 200, nullable = false)
    private String name;
    @Column(name = "description", columnDefinition = "text", nullable = false)
    private String description;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @OneToMany(mappedBy = "roomType")
    @BatchSize(size = 20)
    @OrderBy("displayOrder ASC, id ASC")
    private List<RoomPhoto> photos = new ArrayList<>();
    @OneToMany(mappedBy = "roomType")
    @BatchSize(size = 20)
    @OrderBy("displayOrder ASC, id ASC")
    private List<RoomDetail> details = new ArrayList<>();
    @OneToMany(mappedBy = "roomType")
    @BatchSize(size = 20)
    @OrderBy("id ASC")
    private List<Rate> rates = new ArrayList<>();

    @Column(name = "total_units") private Integer totalUnits;
    @Column(name = "available_units") private Integer availableUnits;
    public Integer getTotalUnits() { return totalUnits; }
    public Integer getAvailableUnits() { return availableUnits; }
    public void update(String name, String description, int order, Integer total, Integer available) {
        this.name = name; this.description = description; this.displayOrder = order;
        this.totalUnits = total; this.availableUnits = available;
    }
    protected RoomType() {}

    public RoomType(Hotel hotel, String code, String name, String description, int displayOrder) {
        this.hotel = hotel;
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public Hotel getHotel() { return hotel; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDisplayOrder() { return displayOrder; }
    public List<RoomPhoto> getPhotos() { return photos; }
    public List<RoomDetail> getDetails() { return details; }
    public List<Rate> getRates() { return rates; }
}
