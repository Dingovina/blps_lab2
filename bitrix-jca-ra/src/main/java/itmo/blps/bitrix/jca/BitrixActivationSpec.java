package itmo.blps.bitrix.jca;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;

import java.io.Serializable;

public class BitrixActivationSpec implements ActivationSpec, Serializable {

    private static final long serialVersionUID = 1L;

    private ResourceAdapter resourceAdapter;
    private int pollingIntervalSeconds = 20;
    private int dealCategoryId = 0;

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    public int getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(int pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public int getDealCategoryId() {
        return dealCategoryId;
    }

    public void setDealCategoryId(int dealCategoryId) {
        this.dealCategoryId = dealCategoryId;
    }

    @Override
    public void validate() throws jakarta.resource.spi.InvalidPropertyException {
        if (pollingIntervalSeconds < 10) {
            throw new jakarta.resource.spi.InvalidPropertyException("pollingIntervalSeconds must be >= 10");
        }
    }
}
