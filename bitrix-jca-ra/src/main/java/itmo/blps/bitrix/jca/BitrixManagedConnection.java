package itmo.blps.bitrix.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;
import javax.transaction.xa.XAResource;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BitrixManagedConnection implements ManagedConnection {

    private final BitrixManagedConnectionFactory mcf;
    private final BitrixConnectionImpl connection;
    private final List<BitrixConnectionEventListener> listeners = new ArrayList<>();
    private PrintWriter logWriter;

    public BitrixManagedConnection(BitrixManagedConnectionFactory mcf, BitrixConnectionImpl connection) {
        this.mcf = mcf;
        this.connection = connection;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        return connection;
    }

    @Override
    public void destroy() throws ResourceException {
        connection.close();
    }

    @Override
    public void cleanup() throws ResourceException {
        connection.close();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof BitrixConnectionImpl)) {
            throw new ResourceException("Invalid connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(jakarta.resource.spi.ConnectionEventListener listener) {
        listeners.add(new BitrixConnectionEventListener(listener));
    }

    @Override
    public void removeConnectionEventListener(jakarta.resource.spi.ConnectionEventListener listener) {
        listeners.removeIf(l -> l.delegate == listener);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new BitrixManagedConnectionMetaData();
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    BitrixConnectionImpl getPhysicalConnection() {
        return connection;
    }

    private static final class BitrixConnectionEventListener {
        private final jakarta.resource.spi.ConnectionEventListener delegate;

        private BitrixConnectionEventListener(jakarta.resource.spi.ConnectionEventListener delegate) {
            this.delegate = delegate;
        }
    }
}
