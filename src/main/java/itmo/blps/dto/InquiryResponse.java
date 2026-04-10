package itmo.blps.dto;

import itmo.blps.entity.Inquiry;

import java.time.Instant;

public class InquiryResponse {

    private Long id;
    private Long listingId;
    private Long buyerId;
    private String message;
    private String status;
    private Instant scheduledAt;
    private String contactInfo;
    private String rejectReason;
    private Boolean willBuy;
    private Instant createdAt;

    public static InquiryResponse from(Inquiry i) {
        InquiryResponse r = new InquiryResponse();
        r.setId(i.getId());
        r.setListingId(i.getListing().getId());
        r.setBuyerId(i.getBuyer().getId());
        r.setMessage(i.getMessage());
        r.setStatus(i.getStatus().name());
        r.setScheduledAt(i.getScheduledAt());
        r.setContactInfo(i.getContactInfo());
        r.setRejectReason(i.getRejectReason());
        r.setWillBuy(i.getWillBuy());
        r.setCreatedAt(i.getCreatedAt());
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

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Boolean getWillBuy() {
        return willBuy;
    }

    public void setWillBuy(Boolean willBuy) {
        this.willBuy = willBuy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
