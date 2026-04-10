package itmo.blps.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import itmo.blps.entity.PromotionType;

public class PromotionPayRequest {

    @NotNull(message = "Promotion type is required")
    private PromotionType promotionType;

    @AssertTrue(message = "Promotion type must be TOP or PREMIUM")
    private boolean isPromotionTypeValid() {
        return promotionType == null
                || promotionType == PromotionType.TOP
                || promotionType == PromotionType.PREMIUM;
    }

    private String returnUrl;
    private String cancelUrl;

    public PromotionType getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(PromotionType promotionType) {
        this.promotionType = promotionType;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }
}
