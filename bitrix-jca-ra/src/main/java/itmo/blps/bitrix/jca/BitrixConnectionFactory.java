package itmo.blps.bitrix.jca;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

import javax.naming.Reference;

public class BitrixConnectionFactory implements BitrixConnectionFactoryInterface, ConnectionFactory {

    private final BitrixManagedConnectionFactory mcf;
    private final ConnectionManager connectionManager;

    public BitrixConnectionFactory(BitrixManagedConnectionFactory mcf, ConnectionManager connectionManager) {
        this.mcf = mcf;
        this.connectionManager = connectionManager;
    }

    @Override
    public BitrixConnection getBitrixConnection() throws ResourceException {
        if (connectionManager != null) {
            return (BitrixConnection) connectionManager.allocateConnection(mcf, null);
        }
        return mcf.createConnection();
    }

    @Override
    public Connection getConnection() throws ResourceException {
        throw new NotSupportedException("Use getBitrixConnection() for Bitrix CCI");
    }

    @Override
    public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
        throw new NotSupportedException("ConnectionSpec not supported; use getBitrixConnection()");
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }

    @Override
    public Reference getReference() {
        return new Reference(BitrixConnectionFactory.class.getName(),
                BitrixManagedConnectionFactory.class.getName(), null);
    }

    @Override
    public void setReference(Reference reference) {
        // embedded deployment — no JNDI
    }
}
