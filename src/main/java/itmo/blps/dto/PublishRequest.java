package itmo.blps.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PublishRequest {

    private boolean forceReject = false;
}
