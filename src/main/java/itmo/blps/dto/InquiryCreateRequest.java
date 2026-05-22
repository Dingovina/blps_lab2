package itmo.blps.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;
}
