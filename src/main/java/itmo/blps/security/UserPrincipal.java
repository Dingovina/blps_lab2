package itmo.blps.security;

import itmo.blps.entity.User;
import java.security.Principal;

public class UserPrincipal implements Principal {
    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    public User getUser() {
        return user;
    }
}
