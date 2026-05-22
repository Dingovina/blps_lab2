package itmo.blps.dto;

import itmo.blps.entity.Listing;
import itmo.blps.entity.ListingStatus;
import itmo.blps.entity.PromotionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
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
}
