package itmo.blps.integration.crm;

import itmo.blps.bitrix.jca.BitrixConnection;
import itmo.blps.bitrix.jca.BitrixConnectionFactoryInterface;
import itmo.blps.bitrix.jca.BitrixEventRecord;
import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;
import itmo.blps.config.BitrixProperties;
import jakarta.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

@Service
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(BitrixWebhookHandler.class);

    static final String AUTH_TOKEN_PARAM = "auth[application_token]";
    static final String DEAL_ID_PARAM = "data[FIELDS][ID]";

    private final BitrixProperties properties;
    private final BitrixConnectionFactoryInterface connectionFactory;
    private final CrmSyncService crmSyncService;
    private final Executor bitrixWebhookExecutor;

    public BitrixWebhookHandler(BitrixProperties properties,
                                BitrixConnectionFactoryInterface connectionFactory,
                                CrmSyncService crmSyncService,
                                @Qualifier("bitrixWebhookExecutor") Executor bitrixWebhookExecutor) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
        this.crmSyncService = crmSyncService;
        this.bitrixWebhookExecutor = bitrixWebhookExecutor;
    }

    public boolean isValidToken(Map<String, String> params) {
        String expected = properties.getWebhookApplicationToken();
        if (!StringUtils.hasText(expected)) {
            return false;
        }
        return Objects.equals(expected, params.get(AUTH_TOKEN_PARAM));
    }

    public void handleEventAsync(Map<String, String> params) {
        bitrixWebhookExecutor.execute(() -> {
            try {
                handleEvent(params);
            } catch (RuntimeException e) {
                log.warn("Bitrix webhook processing failed: {}", e.getMessage());
            }
        });
    }

    private void handleEvent(Map<String, String> params) {
        String event = params.get("event");
        String dealIdRaw = params.get(DEAL_ID_PARAM);
        if (!StringUtils.hasText(dealIdRaw)) {
            log.warn("Bitrix webhook missing deal id, event={}", event);
            return;
        }

        int dealId = Integer.parseInt(dealIdRaw);
        if ("ONCRMDEALDELETE".equals(event)) {
            crmSyncService.applyInboundDealDeleted(dealId);
            return;
        }
        if ("ONCRMDEALADD".equals(event) || "ONCRMDEALUPDATE".equals(event)) {
            handleDealUpsert(dealId);
            return;
        }
        log.debug("Ignoring Bitrix webhook event: {}", event);
    }

    private void handleDealUpsert(int dealId) {
        try (BitrixConnection conn = connectionFactory.getBitrixConnection()) {
            Optional<BitrixDealSnapshot> dealOpt = conn.getDeal(dealId);
            if (dealOpt.isEmpty()) {
                log.warn("Bitrix deal {} not found", dealId);
                return;
            }
            BitrixDealSnapshot deal = dealOpt.get();
            if (!matchesDealCategory(deal)) {
                return;
            }
            crmSyncService.applyInboundDeal(new BitrixEventRecord(deal));
        } catch (ResourceException e) {
            log.error("Bitrix webhook connection failed for deal {}", dealId, e);
        }
    }

    private boolean matchesDealCategory(BitrixDealSnapshot deal) {
        Object category = deal.getField("CATEGORY_ID");
        if (category == null) {
            return true;
        }
        return toInt(category) == properties.getDealCategoryId();
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
