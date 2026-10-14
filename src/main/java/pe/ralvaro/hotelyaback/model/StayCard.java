package pe.ralvaro.hotelyaback.model;

import jakarta.persistence.*;

@Entity @Table(name="stay_card")
public class StayCard {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="hotel_id", nullable=false) private Long hotelId;
    @Column(nullable=false, length=200) private String title;
    @Column(nullable=false, columnDefinition="text") private String description;
    @Column(name="display_order", nullable=false) private int displayOrder;
    protected StayCard() {}
    public StayCard(Long hotelId, String title, String description, int order) {
        this.hotelId=hotelId; this.title=title; this.description=description; this.displayOrder=order;
    }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
