package itmo.blps.dto;

import itmo.blps.entity.PromotionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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
}
