package com.satish.urlshortener.exception;

import com.satish.urlshortener.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * One central place that turns exceptions into clean JSON error answers.
 * Every controller in the app uses these rules automatically.
 *
 * Mapping:
 * - InvalidUrlException            -> 400 Bad Request
 * - validation errors (@Valid)     -> 400 Bad Request
 * - body missing / broken JSON     -> 400 Bad Request
 * - UrlNotFoundException           -> 404 Not Found
 * - UrlExpiredException            -> 410 Gone
 * - ShortCodeGenerationException   -> 500 Internal Server Error
 * - any other exception            -> 500, without showing details
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** User sent a bad URL -> 400 with the reason. */
    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, e.getMessage()));
    }

    /** @Valid checks failed (empty url, too long) -> 400 with the first message. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Request body is not valid");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, message));
    }

    /** Body missing or not valid JSON -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Request body is missing or not valid JSON"));
    }

    /** Short code does not exist -> 404. */
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UrlNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, e.getMessage()));
    }

    /** Link exists but expiry time has passed -> 410 Gone. */
    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(UrlExpiredException e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ErrorResponse.of(410, e.getMessage()));
    }

    /** Could not create a unique code -> 500. */
    @ExceptionHandler(ShortCodeGenerationException.class)
    public ResponseEntity<ErrorResponse> handleGeneration(ShortCodeGenerationException e) {
        log.error("Short code generation failed", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(500, e.getMessage()));
    }

    /**
     * Any other unexpected error -> 500.
     * We log the full details for ourselves,
     * but send only a general message to the user.
     * Reason: internal details in error answers can help attackers.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(500, "Something went wrong. Please try again."));
    }
}