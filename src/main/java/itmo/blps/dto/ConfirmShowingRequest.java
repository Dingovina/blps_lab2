package itmo.blps.dto;

import java.time.Instant;

public class ConfirmShowingRequest {

    private Instant scheduledAt;
    private String contactInfo;

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
}
