package itmo.blps.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bitrix")
public class BitrixProperties {

    private boolean enabled;
    private String restBaseUrl;
    private int pollingIntervalSeconds;
    private int dealCategoryId;
    private String dealFieldListingId;
    private String dealFieldPromotion;
    private String dealFieldAddress;
    private String dealFieldArea;
    private String dealFieldRooms;
    private String stagePublish;
    private String stageActive;
    private String stageClosed;
    private String stageArchived;
    private boolean backfillOnStartup;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRestBaseUrl() {
        return restBaseUrl;
    }

    public void setRestBaseUrl(String restBaseUrl) {
        this.restBaseUrl = restBaseUrl;
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

    public String getDealFieldAddress() {
        return dealFieldAddress;
    }

    public void setDealFieldAddress(String dealFieldAddress) {
        this.dealFieldAddress = dealFieldAddress;
    }

    public String getDealFieldArea() {
        return dealFieldArea;
    }

    public void setDealFieldArea(String dealFieldArea) {
        this.dealFieldArea = dealFieldArea;
    }

    public String getDealFieldRooms() {
        return dealFieldRooms;
    }

    public void setDealFieldRooms(String dealFieldRooms) {
        this.dealFieldRooms = dealFieldRooms;
    }

    public String getStagePublish() {
        return stagePublish;
    }

    public void setStagePublish(String stagePublish) {
        this.stagePublish = stagePublish;
    }

    public String getStageActive() {
        return stageActive;
    }

    public void setStageActive(String stageActive) {
        this.stageActive = stageActive;
    }

    public String getStageClosed() {
        return stageClosed;
    }

    public void setStageClosed(String stageClosed) {
        this.stageClosed = stageClosed;
    }

    public String getStageArchived() {
        return stageArchived;
    }

    public void setStageArchived(String stageArchived) {
        this.stageArchived = stageArchived;
    }

    public boolean isBackfillOnStartup() {
        return backfillOnStartup;
    }

    public void setBackfillOnStartup(boolean backfillOnStartup) {
        this.backfillOnStartup = backfillOnStartup;
    }
}
