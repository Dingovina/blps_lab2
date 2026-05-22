package itmo.blps.dto;

import itmo.blps.entity.ListingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminListingStatusRequest {

    @NotNull(message = "Status is required")
    private ListingStatus status;
}
