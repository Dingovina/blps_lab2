package itmo.blps.config;

import itmo.blps.bitrix.jca.embedded.BitrixJcaEmbeddedContainer;
import itmo.blps.integration.crm.BitrixInboundSyncService;
import jakarta.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixInboundActivator {

    private static final Logger log = LoggerFactory.getLogger(BitrixInboundActivator.class);

    private final BitrixJcaEmbeddedContainer container;
    private final BitrixInboundSyncService inboundSyncService;
    private final BitrixProperties properties;

    public BitrixInboundActivator(BitrixJcaEmbeddedContainer container,
                                  BitrixInboundSyncService inboundSyncService,
                                  BitrixProperties properties) {
        this.container = container;
        this.inboundSyncService = inboundSyncService;
        this.properties = properties;
    }

    @Order(200)
    @EventListener(ApplicationReadyEvent.class)
    public void activateInbound() throws ResourceException {
        container.activateInbound(
                inboundSyncService,
                properties.getPollingIntervalSeconds(),
                properties.getDealCategoryId()
        );
        log.info("Bitrix inbound listener activated");
    }
}
