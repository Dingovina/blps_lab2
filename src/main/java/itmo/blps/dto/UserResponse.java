package itmo.blps.dto;

import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private UserRole role;
    private Instant createdAt;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }
}
