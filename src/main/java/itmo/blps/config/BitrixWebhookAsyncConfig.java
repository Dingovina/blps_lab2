package itmo.blps.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixWebhookAsyncConfig {

    @Bean(name = "bitrixWebhookExecutor")
    public Executor bitrixWebhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("bitrix-webhook-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
