package itmo.blps.config;

import itmo.blps.bitrix.jca.BitrixConnectionFactoryInterface;
import itmo.blps.bitrix.jca.embedded.BitrixJcaEmbeddedContainer;
import jakarta.annotation.PreDestroy;
import jakarta.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile("bitrix")
@EnableConfigurationProperties(BitrixProperties.class)
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixJcaConfig {

    private static final Logger log = LoggerFactory.getLogger(BitrixJcaConfig.class);

    private BitrixJcaEmbeddedContainer container;

    @Bean
    public BitrixJcaEmbeddedContainer bitrixJcaEmbeddedContainer(BitrixProperties properties) throws ResourceException {
        if (!StringUtils.hasText(properties.getRestBaseUrl())) {
            throw new IllegalStateException("app.bitrix.rest-base-url (BITRIX_REST_BASE_URL) is required for profile bitrix");
        }
        container = new BitrixJcaEmbeddedContainer(
                properties.getRestBaseUrl(),
                properties.getDealFieldListingId(),
                properties.getDealFieldPromotion(),
                properties.getDealCategoryId()
        );
        log.info("Bitrix JCA embedded container created");
        return container;
    }

    @Bean
    public BitrixConnectionFactoryInterface bitrixConnectionFactory(BitrixJcaEmbeddedContainer container) {
        return container.getConnectionFactory();
    }

    @PreDestroy
    public void shutdown() {
        if (container != null) {
            container.close();
        }
    }
}
