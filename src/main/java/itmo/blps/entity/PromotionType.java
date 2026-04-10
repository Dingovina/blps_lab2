package itmo.blps.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PromotionType {
    NONE,
    TOP,
    PREMIUM;

    @JsonCreator
    public static PromotionType fromValue(String value) {
        if (value == null) return null;
        for (PromotionType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Invalid promotionType '" + value + "'. Allowed values: TOP, PREMIUM");
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
