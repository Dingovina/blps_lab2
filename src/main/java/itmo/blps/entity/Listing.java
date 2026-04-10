package itmo.blps.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cian_listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "region", length = 255)
    private String region;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "area_sqm", precision = 10, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "rooms")
    private Integer rooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ListingStatus status = ListingStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion", nullable = false)
    private PromotionType promotion = PromotionType.NONE;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(BigDecimal areaSqm) {
        this.areaSqm = areaSqm;
    }

    public Integer getRooms() {
        return rooms;
    }

    public void setRooms(Integer rooms) {
        this.rooms = rooms;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public PromotionType getPromotion() {
        return promotion;
    }

    public void setPromotion(PromotionType promotion) {
        this.promotion = promotion;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
