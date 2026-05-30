package itmo.blps.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String from;
    private String brandName = "Cian";
    private String supportEmail = "support@cian.local";
}
