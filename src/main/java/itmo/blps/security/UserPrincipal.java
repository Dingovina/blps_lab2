package itmo.blps.security;

import itmo.blps.entity.User;
import lombok.Getter;

import java.security.Principal;

@Getter
public class UserPrincipal implements Principal {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
