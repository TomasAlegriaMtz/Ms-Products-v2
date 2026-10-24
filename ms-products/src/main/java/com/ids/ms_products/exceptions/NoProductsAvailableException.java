package com.ids.ms_products.exceptions;

public class NoProductsAvailableException extends RuntimeException{
    public NoProductsAvailableException(String message) {
        super(message);
    }
}
