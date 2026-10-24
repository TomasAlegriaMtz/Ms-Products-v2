package com.ids.ms_products.exceptions;

public class InvalidCsvException extends RuntimeException {

    public InvalidCsvException(String mensaje) {
        super(mensaje);
    }
}