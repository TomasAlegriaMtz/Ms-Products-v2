package com.ids.ms_products.exceptions;

public class ProductNotValidException extends RuntimeException {
    public ProductNotValidException(String message) {
        super(message);
    }
}
