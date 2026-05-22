package itmo.blps.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class ShowingDecisionRequest {

    public enum Decision { CONFIRM, REJECT }

    @NotNull
    private Decision decision;

    private Instant scheduledAt;

    @Size(max = 500, message = "Contact info must not exceed 500 characters")
    private String contactInfo;

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;
}
