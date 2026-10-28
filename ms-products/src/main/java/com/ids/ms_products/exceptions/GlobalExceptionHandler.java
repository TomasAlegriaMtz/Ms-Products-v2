
package com.ids.ms_products.exceptions;

import com.ids.ms_products.dto.ErrorResponse;
import com.ids.ms_products.repository.StockMovementActorNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import static com.ids.ms_products.constants.ErrorMessages.ACCESS_DENIED;
import static com.ids.ms_products.constants.ErrorMessages.ACCESS_DENIED_CODE;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                ex.getMessage(), request);
    }

    @ExceptionHandler(ProductAlreadyInactiveException.class)
    public ResponseEntity<ErrorResponse> handleProductAlreadyInactive(
            ProductAlreadyInactiveException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "PRODUCT_ALREADY_INACTIVE",
                ex.getMessage(), request);
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProductAlreadyExists(
            ProductAlreadyExistsException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "PRODUCT_ALREADY_EXISTS",
                ex.getMessage(), request);
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePromotionNotFound(
            PromotionNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "PROMOTION_NOT_FOUND",
                ex.getMessage(), request);
    }

    @ExceptionHandler(PromotionConflictException.class)
    public ResponseEntity<ErrorResponse> handlePromotionConflict(
            PromotionConflictException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "PROMOTION_CONFLICT",
                ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                ex.getMessage(), request);
    }

    @ExceptionHandler(StockMovementActorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockMovementActorNotFound(
            StockMovementActorNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "STOCK_MOVEMENT_ACTOR_NOT_FOUND",
                ex.getMessage(), request);
    }

    @ExceptionHandler(PromotionException.class)
    public ResponseEntity<ErrorResponse> handlePromotionException(
            PromotionException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "PROMOTION_ERROR",
                ex.getMessage(), request);
    }

    @ExceptionHandler(NoProductsAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoProducts(
            NoProductsAvailableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "NO_PRODUCTS_AVAILABLE",
                ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));

        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Uno o más parámetros de la solicitud no son válidos",
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonError(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "El cuerpo de la solicitud contiene valores inválidos o mal formados",
                request);
    }

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ErrorResponse> handleProductException(
            ProductException ex, HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, "PRODUCT_ERROR",
                ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCsvException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCsv(
            InvalidCsvException ex, HttpServletRequest request) {

        return buildError(HttpStatus.BAD_REQUEST, "INVALID_CSV",
                ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE",
                "El archivo supera el tamaño máximo permitido de 5 MB",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(
            Exception ex, HttpServletRequest request) {

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Ocurrió un error interno en el servidor",
                request);
    }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                AccessDeniedException ex,
                HttpServletRequest request) {

        return buildError(
                HttpStatus.FORBIDDEN,
                ACCESS_DENIED_CODE,
                ex.getMessage(),
                request
        );
        }
}
