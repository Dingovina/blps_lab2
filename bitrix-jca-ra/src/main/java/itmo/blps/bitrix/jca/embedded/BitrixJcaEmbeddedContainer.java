package itmo.blps.bitrix.jca.embedded;

import itmo.blps.bitrix.jca.BitrixActivationSpec;
import itmo.blps.bitrix.jca.BitrixConnectionFactory;
import itmo.blps.bitrix.jca.BitrixConnectionFactoryInterface;
import itmo.blps.bitrix.jca.BitrixEventListener;
import itmo.blps.bitrix.jca.BitrixManagedConnectionFactory;
import itmo.blps.bitrix.jca.BitrixResourceAdapter;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.endpoint.MessageEndpoint;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

    public void activateInbound(BitrixEventListener listener, int pollingIntervalSeconds, int dealCategoryId)
            throws ResourceException {
        BitrixActivationSpec spec = new BitrixActivationSpec();
        spec.setPollingIntervalSeconds(pollingIntervalSeconds);
        spec.setDealCategoryId(dealCategoryId);

        MessageEndpointFactory factory = new MessageEndpointFactory() {
            @Override
            public MessageEndpoint createEndpoint(javax.transaction.xa.XAResource xaResource)
                    throws jakarta.resource.spi.UnavailableException {
                return createEndpointProxy(listener);
            }

            @Override
            public MessageEndpoint createEndpoint(javax.transaction.xa.XAResource xaResource, long timeout)
                    throws jakarta.resource.spi.UnavailableException {
                return createEndpointProxy(listener);
            }

            @Override
            public Class<?> getEndpointClass() {
                return BitrixEventListener.class;
            }

            @Override
            public String getActivationName() {
                return "bitrix-inbound";
            }

            @Override
            public boolean isDeliveryTransacted(Method method) {
                return false;
            }
        };

        resourceAdapter.endpointActivation(factory, spec);
    }

    @Override
    public void close() {
        resourceAdapter.stop();
    }

    private static MessageEndpoint createEndpointProxy(BitrixEventListener listener) {
        return (MessageEndpoint) Proxy.newProxyInstance(
                BitrixEventListener.class.getClassLoader(),
                new Class[]{MessageEndpoint.class, BitrixEventListener.class},
                new ListenerInvocationHandler(listener));
    }

    private static final class ListenerInvocationHandler implements InvocationHandler {

        private final BitrixEventListener delegate;

        private ListenerInvocationHandler(BitrixEventListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("onDealUpdated".equals(name) && args != null && args.length == 1) {
                delegate.onDealUpdated((itmo.blps.bitrix.jca.BitrixEventRecord) args[0]);
                return null;
            }
            if ("beforeDelivery".equals(name) || "afterDelivery".equals(name) || "release".equals(name)) {
                return null;
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "BitrixMessageEndpointProxy";
            }
            return null;
        }
    }
}
