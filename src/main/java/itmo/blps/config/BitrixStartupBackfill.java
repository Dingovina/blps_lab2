package itmo.blps.config;

import itmo.blps.integration.crm.CrmSyncService;
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
public class BitrixStartupBackfill {

    private static final Logger log = LoggerFactory.getLogger(BitrixStartupBackfill.class);

    private final CrmSyncService crmSyncService;
    private final BitrixProperties properties;

    public BitrixStartupBackfill(CrmSyncService crmSyncService, BitrixProperties properties) {
        this.crmSyncService = crmSyncService;
        this.properties = properties;
    }

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        if (!properties.isBackfillOnStartup()) {
            log.info("Bitrix startup backfill disabled (app.bitrix.backfill-on-startup=false)");
            return;
        }
        log.info("Starting Bitrix startup backfill...");
        crmSyncService.backfillExistingData();
        crmSyncService.reconcileDeletedDeals();
    }
}
