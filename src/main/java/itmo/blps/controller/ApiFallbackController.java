package itmo.blps.controller;

import itmo.blps.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catch-all for any /api/** path that has no matching controller.
 * Spring evaluates this mapping last because "/**" has the lowest specificity.
 */
@RestController
@RequestMapping("/api")
public class ApiFallbackController {

    @RequestMapping("/**")
    public ResponseEntity<ErrorResponse> notFound(HttpServletRequest request) {
        String message = "No endpoint: " + request.getMethod() + " " + request.getRequestURI();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", message));
    }
}
