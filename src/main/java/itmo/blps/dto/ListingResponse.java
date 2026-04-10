package itmo.blps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.PromotionType;

import java.math.BigDecimal;
import java.time.Instant;

public class ListingResponse {

    private Long id;
    private Long sellerId;
    private String title;
    private String description;
    private String address;
    private String region;
    private BigDecimal price;
    private BigDecimal areaSqm;
    private Integer rooms;
    private ListingStatus status;
    private PromotionType promotion;
    private Instant publishedAt;
    private Instant expiresAt;
    private Instant closedAt;
    private Instant createdAt;

    public static ListingResponse from(Listing l) {
        ListingResponse r = new ListingResponse();
        r.setId(l.getId());
        r.setSellerId(l.getSeller() != null ? l.getSeller().getId() : null);
        r.setTitle(l.getTitle());
        r.setDescription(l.getDescription());
        r.setAddress(l.getAddress());
        r.setRegion(l.getRegion());
        r.setPrice(l.getPrice());
        r.setAreaSqm(l.getAreaSqm());
        r.setRooms(l.getRooms());
        r.setStatus(l.getStatus());
        r.setPromotion(l.getPromotion());
        r.setPublishedAt(l.getPublishedAt());
        r.setExpiresAt(l.getExpiresAt());
        r.setClosedAt(l.getClosedAt());
        r.setCreatedAt(l.getCreatedAt());
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("sellerId")
    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
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

    @JsonProperty("areaSqm")
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

    @JsonProperty("publishedAt")
    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    @JsonProperty("expiresAt")
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonProperty("closedAt")
    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    @JsonProperty("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
