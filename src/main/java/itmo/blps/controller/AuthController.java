package itmo.blps.controller;

import itmo.blps.dto.LoginRequest;
import itmo.blps.dto.RegisterRequest;
import itmo.blps.dto.UserResponse;
import itmo.blps.entity.User;
import itmo.blps.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<java.util.Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(java.util.Map.of("accessToken", authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = itmo.blps.security.AuthUtil.currentUser();
        return ResponseEntity.ok(authService.me(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}
