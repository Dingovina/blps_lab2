package itmo.blps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private String notificationsQueue;
    private String listenerConcurrency;
    private final Rabbitmq rabbitmq = new Rabbitmq();

    public String getNotificationsQueue() {
        return notificationsQueue;
    }

    public void setNotificationsQueue(String notificationsQueue) {
        this.notificationsQueue = notificationsQueue;
    }

    public String getListenerConcurrency() {
        return listenerConcurrency;
    }

    public void setListenerConcurrency(String listenerConcurrency) {
        this.listenerConcurrency = listenerConcurrency;
    }

    public Rabbitmq getRabbitmq() {
        return rabbitmq;
    }

    public static class Rabbitmq {

        private String host;
        private int port;
        private String username;
        private String password;
        private String virtualHost;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getVirtualHost() {
            return virtualHost;
        }

        public void setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
        }
    }
}
