package itmo.blps.dto;

public class ConfirmRelevanceRequest {

    private boolean relevant = true;

    public boolean isRelevant() {
        return relevant;
    }

    public void setRelevant(boolean relevant) {
        this.relevant = relevant;
    }
}
