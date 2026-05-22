package itmo.blps.entity;

import jakarta.persistence.*;
import java.time.Instant;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getPublishedListings() {
        return publishedListings;
    }

    public void setPublishedListings(Integer publishedListings) {
        this.publishedListings = publishedListings;
    }

    public Integer getClosedListings() {
        return closedListings;
    }

    public void setClosedListings(Integer closedListings) {
        this.closedListings = closedListings;
    }

    public Integer getCompletedInquiries() {
        return completedInquiries;
    }

    public void setCompletedInquiries(Integer completedInquiries) {
        this.completedInquiries = completedInquiries;
    }

    public Integer getScheduledInquiries() {
        return scheduledInquiries;
    }

    public void setScheduledInquiries(Integer scheduledInquiries) {
        this.scheduledInquiries = scheduledInquiries;
    }

    public Integer getShowRequests() {
        return showRequests;
    }

    public void setShowRequests(Integer showRequests) {
        this.showRequests = showRequests;
    }

    public Integer getScheduledShowings() {
        return scheduledShowings;
    }

    public void setScheduledShowings(Integer scheduledShowings) {
        this.scheduledShowings = scheduledShowings;
    }

    public Integer getRejectedShowings() {
        return rejectedShowings;
    }

    public void setRejectedShowings(Integer rejectedShowings) {
        this.rejectedShowings = rejectedShowings;
    }

    public Integer getCompletedShowings() {
        return completedShowings;
    }

    public void setCompletedShowings(Integer completedShowings) {
        this.completedShowings = completedShowings;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
