package itmo.blps.exception;

import itmo.blps.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        ErrorResponse body = new ErrorResponse("VALIDATION_ERROR", "Validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(org.springframework.security.authorization.AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", "Access Denied"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", "Access Denied"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Invalid email or password"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof IllegalArgumentException) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("BAD_REQUEST", t.getMessage()));
            }
            t = t.getCause();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", "Invalid request body"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("UNSUPPORTED_MEDIA_TYPE",
                        "Content-Type '" + ex.getContentType() + "' not supported. Use 'application/json'"));
    }

    @ExceptionHandler({ IllegalArgumentException.class, BadRequestException.class })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
