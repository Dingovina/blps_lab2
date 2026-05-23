package itmo.blps.integration.crm;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CrmSyncHelper {

    public void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
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
