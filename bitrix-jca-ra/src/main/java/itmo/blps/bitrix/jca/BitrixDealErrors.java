package itmo.blps.bitrix.jca;

public final class BitrixDealErrors {

    private BitrixDealErrors() {
    }

    public static boolean isNotFound(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("Not found")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
