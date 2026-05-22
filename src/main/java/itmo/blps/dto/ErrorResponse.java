package itmo.blps.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ErrorResponse {

    private ErrorBody error;

    public ErrorResponse(String code, String message) {
        this.error = new ErrorBody(code, message, null);
    }

    public ErrorResponse(String code, String message, List<String> details) {
        this.error = new ErrorBody(code, message, details);
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;
        private List<String> details;
    }
}
