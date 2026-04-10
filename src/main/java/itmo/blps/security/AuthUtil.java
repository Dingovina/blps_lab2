package itmo.blps.security;

import itmo.blps.config.SpringContext;
import itmo.blps.entity.User;
import itmo.blps.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to get the currently authenticated User entity.
 * Works with JAAS authentication where the principal is a plain String (email).
 */
public final class AuthUtil {

    private AuthUtil() {}

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        String email = auth.getName();
        UserRepository userRepository = SpringContext.getBean(UserRepository.class);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }
}
