package itmo.blps.dto;

import itmo.blps.entity.Payment;

import java.time.Instant;

public class PaymentResponse {

    private Long id;
    private Long listingId;
    private Long userId;
    private String promotionType;
    private String status;
    private String externalId;
    private Integer amountCents;
    private Instant createdAt;

    public static PaymentResponse from(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setListingId(p.getListing().getId());
        r.setUserId(p.getUser().getId());
        r.setPromotionType(p.getPromotionType().name());
        r.setStatus(p.getStatus().name());
        r.setExternalId(p.getExternalId());
        r.setAmountCents(p.getAmountCents());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(String promotionType) {
        this.promotionType = promotionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
