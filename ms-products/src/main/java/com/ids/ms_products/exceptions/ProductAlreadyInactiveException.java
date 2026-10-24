package com.ids.ms_products.exceptions;

public class ProductAlreadyInactiveException extends RuntimeException {
    public ProductAlreadyInactiveException(String message) {
        super(message);
    }
}
