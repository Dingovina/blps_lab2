package itmo.blps.dto;

import itmo.blps.entity.Payment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
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
}
