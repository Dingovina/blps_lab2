package itmo.blps.security;

import itmo.blps.config.SpringContext;
import itmo.blps.entity.User;
import itmo.blps.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;

public class AppLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean loginSucceeded = false;
    private UserPrincipal userPrincipal;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available");
        }

        NameCallback nameCallback = new NameCallback("username: ");
        PasswordCallback passwordCallback = new PasswordCallback("password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
            String username = nameCallback.getName();
            String password = new String(passwordCallback.getPassword());

            UserRepository userRepository = SpringContext.getBean(UserRepository.class);
            PasswordEncoder passwordEncoder = SpringContext.getBean(PasswordEncoder.class);

            User user = userRepository.findByEmail(username).orElse(null);
            if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
                loginSucceeded = true;
                userPrincipal = new UserPrincipal(user);
                return true;
            }

            loginSucceeded = false;
            throw new LoginException("Authentication failed");

        } catch (IOException | UnsupportedCallbackException | NullPointerException e) {
            throw new LoginException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }
        if (!subject.getPrincipals().contains(userPrincipal)) {
            subject.getPrincipals().add(userPrincipal);
        }
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }
        logout();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        loginSucceeded = false;
        userPrincipal = null;
        return true;
    }
}
