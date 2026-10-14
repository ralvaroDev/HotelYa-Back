package pe.ralvaro.hotelyaback.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "hotel")
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code", length = 80, nullable = false, unique = true)
    private String code;
    @Column(name = "name", length = 200, nullable = false)
    private String name;
    @Column(name = "description", columnDefinition = "text", nullable = false)
    private String description;
    @Column(name = "address", length = 500, nullable = false)
    private String address;
    @Column(name = "location", length = 200, nullable = false)
    private String location;
    @Column(name = "phone", length = 40)
    private String phone;
    @Column(name = "logo_path", length = 500)
    private String logoPath;
    @Column(name = "facade_path", length = 500)
    private String facadePath;
    @Column(name = "video_path", length = 500)
    private String videoPath;
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;
    @Column(name = "whatsapp", length = 40)
    private String whatsapp;
    @Column(name = "opening_hours", columnDefinition = "text")
    private String openingHours;
    @Column(name = "check_in_out_conditions", columnDefinition = "text")
    private String checkInOutConditions;
    @Column(name = "has_parking")
    private Boolean hasParking;

    @Column(name = "map_url", length = 2048)
    private String mapUrl;
    @Column(name = "opening_time")
    private LocalTime openingTime;
    @Column(name = "closing_time")
    private LocalTime closingTime;
    public String getMapUrl() { return mapUrl; }
    public LocalTime getOpeningTime() { return openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void updateLocationAndHours(String url, LocalTime start, LocalTime end) {
        mapUrl = url; openingTime = start; closingTime = end;
        if (start != null) openingHours = start.equals(end) ? "24 horas" : start + " – " + end + (end.isBefore(start) ? " (día siguiente)" : "");
    }

    @Column(name = "owner_id", unique = true)
    private Long ownerId;
    @Column(nullable = false)
    private boolean published = true;
    public Long getOwnerId() { return ownerId; }
    public boolean isPublished() { return published; }
    public void assignOwner(Long id) { ownerId = id; }
    public void setPublished(boolean value) { published = value; }
    public void update(String name, String description, String address, String location, String phone,
            String logo, String facade, String video, BigDecimal lat, BigDecimal lng, String whatsapp,
            String hours, String conditions, Boolean parking) {
        this.name = name; this.description = description; this.address = address; this.location = location;
        this.phone = phone; this.logoPath = logo; this.facadePath = facade; this.videoPath = video;
        this.latitude = lat; this.longitude = lng; this.whatsapp = whatsapp; this.openingHours = hours;
        this.checkInOutConditions = conditions; this.hasParking = parking;
    }
    protected Hotel() {}

    public Hotel(String code, String name, String description, String address, String location, String phone, String logoPath, String facadePath, String videoPath) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.address = address;
        this.location = location;
        this.phone = phone;
        this.logoPath = logoPath;
        this.facadePath = facadePath;
        this.videoPath = videoPath;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getLocation() { return location; }
    public String getPhone() { return phone; }
    public String getLogoPath() { return logoPath; }
    public String getFacadePath() { return facadePath; }
    public String getVideoPath() { return videoPath; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getWhatsapp() { return whatsapp; }
    public String getOpeningHours() { return openingHours; }
    public String getCheckInOutConditions() { return checkInOutConditions; }
    public Boolean getHasParking() { return hasParking; }
}
