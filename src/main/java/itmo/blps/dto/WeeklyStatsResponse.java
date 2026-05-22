package itmo.blps.dto;

import itmo.blps.entity.WeeklyStats;

import java.time.Instant;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
