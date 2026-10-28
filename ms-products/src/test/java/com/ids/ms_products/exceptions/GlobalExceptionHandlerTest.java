package com.ids.ms_products.exceptions;

import com.ids.ms_products.repository.StockMovementActorNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void mapsDomainExceptionsToTheirExpectedStatuses() {
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleProductNotFound(new ProductNotFoundException("missing"), request).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleProductNotValid(new ProductNotValidException("invalid"), request).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleProductAlreadyInactive(new ProductAlreadyInactiveException("inactive"), request).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleProductAlreadyExists(new ProductAlreadyExistsException("duplicate"), request).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handlePromotionNotFound(new PromotionNotFoundException("missing"), request).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handlePromotionConflict(new PromotionConflictException("overlap"), request).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleInsufficientStock(new InsufficientStockException("low stock"), request).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleStockMovementActorNotFound(
                        new StockMovementActorNotFoundException("missing actor"), request).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handlePromotionException(new PromotionException("invalid promotion"), request).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                handler.handleNoProducts(new NoProductsAvailableException("empty"), request).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleProductException(new ProductException("invalid product"), request).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleIllegalArgument(new IllegalArgumentException("invalid arg"), request).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleInvalidCsv(new InvalidCsvException("invalid csv"), request).getStatusCode());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
                handler.handleMaxUploadSize(new MaxUploadSizeExceededException(5000), request).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                handler.handleUnexpectedError(new Exception("unexpected"), request).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleAccessDenied(new AccessDeniedException("access denied"), request).getStatusCode());
    }

    @Test
    void includesValidationFieldMessagesInBadRequestResponse() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "required"));
        bindingResult.addError(new FieldError("request", "price", "must be positive"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                (MethodParameter) null, bindingResult);

        var response = handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // 👇 Sin "get" porque es un record
        assertTrue(response.getBody().message().contains("name: required"));
        assertTrue(response.getBody().message().contains("price: must be positive"));
    }

    @Test
    void returnsStableBadRequestBodyForUnreadableJson() {
        var response = handler.handleJsonError(
                new HttpMessageNotReadableException("invalid json", new IllegalArgumentException(), null), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // 👇 Sin "get" porque es un record
        assertEquals(400, response.getBody().status());
        assertEquals("INVALID_REQUEST_BODY", response.getBody().code());
        assertEquals("El cuerpo de la solicitud contiene valores inválidos o mal formados", response.getBody().message());
    }

    @Test
    void handlesConstraintViolationCorrectly() {
        ConstraintViolationException cve = new ConstraintViolationException("violación de regla", Collections.emptySet());
        var response = handler.handleConstraintViolation(cve, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // 👇 Sin "get" porque es un record
        assertEquals("VALIDATION_ERROR", response.getBody().code());
    }

    @Test
    void invalidStockExceptionRetainsItsMessage() {
        InvalidStockException exception = new InvalidStockException("stock inválido");
        assertEquals("stock inválido", exception.getMessage());
    }
}