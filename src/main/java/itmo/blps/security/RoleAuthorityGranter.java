package itmo.blps.security;

import itmo.blps.config.SpringContext;
import itmo.blps.entity.User;
import itmo.blps.repository.UserRepository;
import org.springframework.security.authentication.jaas.AuthorityGranter;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RoleAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {

        String email = principal.getName();
        UserRepository userRepository = SpringContext.getBean(UserRepository.class);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return Collections.emptySet();
        }

        Set<String> authorities = new HashSet<>();
        authorities.add("ROLE_" + user.getRole().name());

        switch (user.getRole()) {
            case SELLER:
                authorities.add("PRIV_AUTH_ACCESS");
                authorities.add("PRIV_READ_PUBLIC_RESOURCES");
                authorities.add("PRIV_BASIC_ACCESS");
                authorities.add("PRIV_CREATE_LISTING");
                authorities.add("PRIV_MANAGE_OWN_LISTINGS");
                authorities.add("PRIV_MANAGE_INQUIRIES");
                authorities.add("PRIV_MANAGE_NOTIFICATIONS");
                break;
            case BUYER:
                authorities.add("PRIV_AUTH_ACCESS");
                authorities.add("PRIV_READ_PUBLIC_RESOURCES");
                authorities.add("PRIV_BASIC_ACCESS");
                authorities.add("PRIV_CREATE_INQUIRY");
                authorities.add("PRIV_MANAGE_INQUIRIES");
                authorities.add("PRIV_MANAGE_NOTIFICATIONS");
                break;
            case ADMIN:
                authorities.add("PRIV_AUTH_ACCESS");
                authorities.add("PRIV_READ_PUBLIC_RESOURCES");
                authorities.add("PRIV_BASIC_ACCESS");
                authorities.add("PRIV_CREATE_LISTING");
                authorities.add("PRIV_MANAGE_OWN_LISTINGS");
                authorities.add("PRIV_CREATE_INQUIRY");
                authorities.add("PRIV_MANAGE_INQUIRIES");
                authorities.add("PRIV_MANAGE_NOTIFICATIONS");
                authorities.add("PRIV_ADMIN_ACCESS");
                break;
        }

        return authorities;
    }
}
