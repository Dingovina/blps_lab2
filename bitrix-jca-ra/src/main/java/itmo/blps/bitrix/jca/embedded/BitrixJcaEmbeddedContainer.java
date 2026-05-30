package itmo.blps.bitrix.jca.embedded;

import itmo.blps.bitrix.jca.BitrixConnectionFactory;
import itmo.blps.bitrix.jca.BitrixConnectionFactoryInterface;
import itmo.blps.bitrix.jca.BitrixManagedConnectionFactory;
import itmo.blps.bitrix.jca.BitrixResourceAdapter;
import jakarta.resource.ResourceException;

public class BitrixJcaEmbeddedContainer implements AutoCloseable {

    private final BitrixResourceAdapter resourceAdapter;
    private final BitrixManagedConnectionFactory managedConnectionFactory;
    private final BitrixConnectionFactory connectionFactory;

    public BitrixJcaEmbeddedContainer(String restBaseUrl,
                                      String dealFieldListingId,
                                      String dealFieldPromotion,
                                      String dealFieldAddress,
                                      String dealFieldArea,
                                      String dealFieldRooms,
                                      int dealCategoryId) throws ResourceException {
        managedConnectionFactory = new BitrixManagedConnectionFactory(restBaseUrl);
        managedConnectionFactory.setDealFieldListingId(dealFieldListingId);
        managedConnectionFactory.setDealFieldPromotion(dealFieldPromotion);
        managedConnectionFactory.setDealFieldAddress(dealFieldAddress);
        managedConnectionFactory.setDealFieldArea(dealFieldArea);
        managedConnectionFactory.setDealFieldRooms(dealFieldRooms);
        managedConnectionFactory.setDealCategoryId(dealCategoryId);

        resourceAdapter = new BitrixResourceAdapter();
        resourceAdapter.setConnectionFactory(managedConnectionFactory);
        resourceAdapter.start(null);

        connectionFactory = (BitrixConnectionFactory) managedConnectionFactory.createConnectionFactory();
    }

    public BitrixConnectionFactoryInterface getConnectionFactory() {
        return connectionFactory;
    }

    @Override
    public void close() {
        resourceAdapter.stop();
    }
}
