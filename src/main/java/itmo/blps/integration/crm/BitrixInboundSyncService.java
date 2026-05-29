package itmo.blps.integration.crm;

import itmo.blps.bitrix.jca.BitrixEventListener;
import itmo.blps.bitrix.jca.BitrixEventRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixInboundSyncService implements BitrixEventListener {

    private final CrmSyncService crmSyncService;

    public BitrixInboundSyncService(CrmSyncService crmSyncService) {
        this.crmSyncService = crmSyncService;
    }

    @Override
    public void onDealUpdated(BitrixEventRecord event) {
        crmSyncService.applyInboundDeal(event);
    }

    @Override
    public void onPollCycleComplete() {
        crmSyncService.reconcileDeletedDeals();
    }
}
