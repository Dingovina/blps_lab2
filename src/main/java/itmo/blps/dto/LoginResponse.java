package itmo.blps.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponse user;
}
