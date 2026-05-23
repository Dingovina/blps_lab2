package itmo.blps.bitrix.jca;

import itmo.blps.bitrix.jca.rest.BitrixRestClient;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;

public class BitrixManagedConnectionFactory implements ManagedConnectionFactory, Serializable, Referenceable {

    private static final long serialVersionUID = 1L;

    private String restBaseUrl;
    private String dealFieldListingId = "UF_CRM_1779488860318";
    private String dealFieldPromotion = "UF_CRM_1779488339299";
    private int dealCategoryId = 0;
    private PrintWriter logWriter;

    public BitrixManagedConnectionFactory() {
    }

    public BitrixManagedConnectionFactory(String restBaseUrl) {
        this.restBaseUrl = restBaseUrl;
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        BitrixRestClient client = new BitrixRestClient(restBaseUrl);
        BitrixConnectionImpl connection = new BitrixConnectionImpl(client, dealFieldListingId, dealFieldPromotion);
        return new BitrixManagedConnection(this, connection);
    }

    @SuppressWarnings("rawtypes")
    public ManagedConnection matchManagedConnections(Set connectionSet,
                                                     Subject subject,
                                                     ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        for (Object item : connectionSet) {
            ManagedConnection mc = (ManagedConnection) item;
            if (mc instanceof BitrixManagedConnection bmc && !bmc.getPhysicalConnection().isClosed()) {
                return mc;
            }
        }
        return null;
    }

    @Override
    public ConnectionFactory createConnectionFactory() throws ResourceException {
        return createConnectionFactory(null);
    }

    @Override
    public ConnectionFactory createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new BitrixConnectionFactory(this, cm);
    }

    public BitrixConnection createConnection() throws ResourceException {
        ManagedConnection mc = createManagedConnection(null, null);
        return (BitrixConnection) mc.getConnection(null, null);
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    @Override
    public Reference getReference() {
        return new Reference(BitrixManagedConnectionFactory.class.getName(),
                BitrixManagedConnectionFactory.class.getName(), null);
    }

    public String getRestBaseUrl() {
        return restBaseUrl;
    }

    public void setRestBaseUrl(String restBaseUrl) {
        this.restBaseUrl = restBaseUrl;
    }

    public String getDealFieldListingId() {
        return dealFieldListingId;
    }

    public void setDealFieldListingId(String dealFieldListingId) {
        this.dealFieldListingId = dealFieldListingId;
    }

    public String getDealFieldPromotion() {
        return dealFieldPromotion;
    }

    public void setDealFieldPromotion(String dealFieldPromotion) {
        this.dealFieldPromotion = dealFieldPromotion;
    }

    public int getDealCategoryId() {
        return dealCategoryId;
    }

    public void setDealCategoryId(int dealCategoryId) {
        this.dealCategoryId = dealCategoryId;
    }
}
