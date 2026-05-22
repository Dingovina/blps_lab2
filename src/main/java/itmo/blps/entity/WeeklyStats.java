package itmo.blps.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cian_weekly_stats")
public class WeeklyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "published_listings")
    private Integer publishedListings;

    @Column(name = "closed_listings")
    private Integer closedListings;

    @Column(name = "completed_inquiries")
    private Integer completedInquiries;

    @Column(name = "scheduled_inquiries")
    private Integer scheduledInquiries;

    @Column(name = "show_requests")
    private Integer showRequests;

    @Column(name = "scheduled_showings")
    private Integer scheduledShowings;

    @Column(name = "rejected_showings")
    private Integer rejectedShowings;

    @Column(name = "completed_showings")
    private Integer completedShowings;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();
}
