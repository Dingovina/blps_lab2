package itmo.blps.integration.crm;


public final class BitrixSyncContext {

    private static final ThreadLocal<Boolean> INBOUND = ThreadLocal.withInitial(() -> false);

    private BitrixSyncContext() {
    }

    public static boolean isInbound() {
        return Boolean.TRUE.equals(INBOUND.get());
    }

    public static void runInbound(Runnable action) {
        INBOUND.set(true);
        try {
            action.run();
        } finally {
            INBOUND.remove();
        }
    }
}
