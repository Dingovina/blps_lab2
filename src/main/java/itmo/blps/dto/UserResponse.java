package itmo.blps.dto;

import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;

import java.time.Instant;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
