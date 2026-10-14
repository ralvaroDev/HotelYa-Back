package pe.ralvaro.hotelyaback.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rate")
public class Rate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 20, nullable = false)
    private RateMode mode;
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    @Column(name = "package_hours")
    private Integer packageHours;
    @Column(name = "additional_hour_amount", precision = 12, scale = 2)
    private BigDecimal additionalHourAmount;
    @Column(name = "simulated", nullable = false)
    private boolean simulated;

    protected Rate() {}

    public Rate(RoomType roomType, RateMode mode, BigDecimal amount, String currency, Integer packageHours, BigDecimal additionalHourAmount, boolean simulated) {
        this.roomType = roomType;
        this.mode = mode;
        this.amount = amount;
        this.currency = currency;
        this.packageHours = packageHours;
        this.additionalHourAmount = additionalHourAmount;
        this.simulated = simulated;
    }

    public Long getId() { return id; }
    public RoomType getRoomType() { return roomType; }
    public RateMode getMode() { return mode; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Integer getPackageHours() { return packageHours; }
    public BigDecimal getAdditionalHourAmount() { return additionalHourAmount; }
    public boolean getSimulated() { return simulated; }
}
