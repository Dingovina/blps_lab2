package itmo.blps.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InquiryCreateRequest {

    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
