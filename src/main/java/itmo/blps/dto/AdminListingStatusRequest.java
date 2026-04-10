package itmo.blps.dto;

import itmo.blps.entity.ListingStatus;
import jakarta.validation.constraints.NotNull;

public class AdminListingStatusRequest {

    @NotNull(message = "Status is required")
    private ListingStatus status;

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }
}
