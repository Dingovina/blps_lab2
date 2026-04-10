package itmo.blps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password must not exceed 100 characters")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
