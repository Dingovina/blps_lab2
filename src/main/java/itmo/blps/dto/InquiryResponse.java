package itmo.blps.dto;

import itmo.blps.entity.Inquiry;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
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
}
