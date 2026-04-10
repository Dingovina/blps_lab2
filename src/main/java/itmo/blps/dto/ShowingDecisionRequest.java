package itmo.blps.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class ShowingDecisionRequest {

    public enum Decision { CONFIRM, REJECT }

    @NotNull
    private Decision decision;

    // used when decision = CONFIRM
    private Instant scheduledAt;
    @Size(max = 500, message = "Contact info must not exceed 500 characters")
    private String contactInfo;

    // used when decision = REJECT
    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
