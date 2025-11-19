package usc.uscPredict.exception;

import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Global exception handler for all controllers.
 * Implements centralized error handling following Spring REST best practices.
 * Handles validation errors, domain exceptions, and unexpected errors with consistent response format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors for @RequestBody with @Valid annotation.
     * Triggered when request body fails bean validation constraints.
     *
     * @param ex The exception containing validation errors
     * @return ResponseEntity with structured error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors,
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors for @PathVariable and @RequestParam with @Validated annotation.
     * Triggered when path variables or request parameters fail validation constraints.
     *
     * @param ex The exception containing constraint violations
     * @return ResponseEntity with structured error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request parameters",
            errors,
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom not-found exceptions.
     * Returns 404 NOT_FOUND when a requested resource does not exist.
     *
     * @param ex The not-found exception
     * @return ResponseEntity with 404 status and error message
     */
    @ExceptionHandler({
        PredictUsernameNotFoundException.class,
        EventNotFoundException.class,
        OrderNotFoundException.class,
        MarketNotFoundException.class,
        WalletNotFoundException.class,
        PositionNotFoundException.class,
        TransactionNotFoundException.class,
        NoSuchElementException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Resource not found",
            Map.of("error", ex.getMessage()),
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles business logic validation errors.
     * Returns 400 BAD_REQUEST when input fails business rules validation.
     *
     * @param ex The illegal argument exception
     * @return ResponseEntity with 400 status and error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request",
            Map.of("error", ex.getMessage()),
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal state errors (e.g., insufficient balance, invalid state transitions).
     * Returns 409 CONFLICT when operation cannot be performed due to current state.
     *
     * @param ex The illegal state exception
     * @return ResponseEntity with 409 status and error message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Operation not allowed",
            Map.of("error", ex.getMessage()),
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handles JSON Patch operation errors.
     * Returns 400 BAD_REQUEST when a PATCH request contains invalid operations.
     *
     * @param ex The JSON patch exception
     * @return ResponseEntity with 400 status and error message
     */
    @ExceptionHandler(JsonPatchException.class)
    public ResponseEntity<ErrorResponse> handleJsonPatch(JsonPatchException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid patch operation",
            Map.of("error", ex.getMessage()),
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Provides a consistent error response structure for unhandled errors.
     * Returns 500 INTERNAL_SERVER_ERROR for unexpected failures.
     *
     * @param ex The exception that occurred
     * @return ResponseEntity with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            Map.of("error", ex.getMessage()),
            LocalDateTime.now()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
