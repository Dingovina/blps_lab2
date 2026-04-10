package itmo.blps.dto;

public class PublishRequest {

    private boolean forceReject = false;

    public boolean isForceReject() {
        return forceReject;
    }

    public void setForceReject(boolean forceReject) {
        this.forceReject = forceReject;
    }
}
