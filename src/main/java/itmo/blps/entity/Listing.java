package itmo.blps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
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
}
