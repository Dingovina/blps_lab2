package itmo.blps.bitrix.jca;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BitrixResourceAdapter implements ResourceAdapter {

    private static final Logger LOG = Logger.getLogger(BitrixResourceAdapter.class.getName());

    private BitrixManagedConnectionFactory connectionFactory;
    private PrintWriter logWriter;

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
        log(Level.INFO, "Bitrix Resource Adapter started");
    }

    @Override
    public void stop() {
        log(Level.INFO, "Bitrix Resource Adapter stopped");
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        // inbound polling replaced by HTTP webhook
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
        // inbound polling replaced by HTTP webhook
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        throw new NotSupportedException("XA not supported");
    }

    public void setConnectionFactory(BitrixManagedConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public BitrixManagedConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    private void log(Level level, String msg, Object... params) {
        if (logWriter != null) {
            logWriter.println(String.format(msg.replace("{}", "%s"), params));
        } else {
            LOG.log(level, msg, params);
        }
    }
}
