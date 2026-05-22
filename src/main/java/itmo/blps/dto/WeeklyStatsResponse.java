package itmo.blps.dto;

import itmo.blps.entity.WeeklyStats;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class WeeklyStatsResponse {

    private Instant periodStart;
    private Instant periodEnd;
    private String role;
    private Integer publishedListings;
    private Integer closedListings;
    private Integer completedInquiries;
    private Integer scheduledInquiries;
    private Integer showRequests;
    private Integer scheduledShowings;
    private Integer rejectedShowings;
    private Integer completedShowings;
    private Instant generatedAt;

    public static WeeklyStatsResponse from(WeeklyStats stats) {
        WeeklyStatsResponse r = new WeeklyStatsResponse();
        r.setPeriodStart(stats.getPeriodStart());
        r.setPeriodEnd(stats.getPeriodEnd());
        r.setRole(stats.getRole().name());
        r.setPublishedListings(stats.getPublishedListings());
        r.setClosedListings(stats.getClosedListings());
        r.setCompletedInquiries(stats.getCompletedInquiries());
        r.setScheduledInquiries(stats.getScheduledInquiries());
        r.setShowRequests(stats.getShowRequests());
        r.setScheduledShowings(stats.getScheduledShowings());
        r.setRejectedShowings(stats.getRejectedShowings());
        r.setCompletedShowings(stats.getCompletedShowings());
        r.setGeneratedAt(stats.getGeneratedAt());
        return r;
    }
}
