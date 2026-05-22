package itmo.blps.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmShowingRequest {

    private Instant scheduledAt;
    private String contactInfo;
}
