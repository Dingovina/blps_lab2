package itmo.blps.config;

import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import itmo.blps.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeedRunner(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email:admin@cian.local}") String adminEmail,
                           @Value("${app.admin.password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        User admin = new User();
        admin.setEmail(this.adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(this.adminPassword));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }
}
