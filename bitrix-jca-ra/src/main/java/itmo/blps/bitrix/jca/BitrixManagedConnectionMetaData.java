package itmo.blps.bitrix.jca;

import jakarta.resource.spi.ManagedConnectionMetaData;

public class BitrixManagedConnectionMetaData implements ManagedConnectionMetaData {

    @Override
    public String getEISProductName() {
        return "Bitrix24";
    }

    @Override
    public String getEISProductVersion() {
        return "REST";
    }

    @Override
    public int getMaxConnections() {
        return 10;
    }

    @Override
    public String getUserName() {
        return null;
    }
}
