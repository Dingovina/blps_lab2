package itmo.blps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponse user;

    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("accessToken")
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    @JsonProperty("tokenType")
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    @JsonProperty("expiresIn")
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
