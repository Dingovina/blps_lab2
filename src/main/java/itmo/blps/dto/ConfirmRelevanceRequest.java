package itmo.blps.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmRelevanceRequest {

    private boolean relevant = true;
}
