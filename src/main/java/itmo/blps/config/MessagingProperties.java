package itmo.blps.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private String notificationsQueue;
    private String listenerConcurrency;
    private final Rabbitmq rabbitmq = new Rabbitmq();

    @Getter
    @Setter
    public static class Rabbitmq {

        private String host;
        private int port;
        private String username;
        private String password;
        private String virtualHost;
    }
}
