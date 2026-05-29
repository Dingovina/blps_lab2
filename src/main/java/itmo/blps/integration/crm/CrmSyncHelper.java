package itmo.blps.integration.crm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CrmSyncHelper {

    private static final Logger log = LoggerFactory.getLogger(CrmSyncHelper.class);

    public void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    log.warn("Bitrix sync after commit failed: {}", e.getMessage());
                }
            }
        });
    }

    public void afterCommit(Runnable action, CrmSyncService crmSyncService) {
        if (crmSyncService == null) {
            return;
        }
        afterCommit(action);
    }
}
