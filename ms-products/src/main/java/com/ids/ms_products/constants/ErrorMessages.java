package com.ids.ms_products.constants;

public final class ErrorMessages {
    private ErrorMessages() {
        // Private constructor to prevent instantiation
    }

    public static final String PRODUCT_NOT_FOUND = 
        "No se encontró el producto con id %d";
        
    public static final String ACCESS_DENIED = 
        "Acceso denegado: Se requieren permisos de administrador";

    public static final String PRODUCT_ID_INVALID =
            "El ID del producto debe ser mayor que cero";

    public static final String PRODUCT_NOT_FOUND_CODE =
            "PRODUCT_NOT_FOUND";

    public static final String PRODUCT_ID_INVALID_CODE =
            "PRODUCT_ID_INVALID";

    // Errores generales
    public static final String VALIDATION_ERROR =
            "Los datos enviados no son válidos";

    public static final String VALIDATION_ERROR_CODE =
            "VALIDATION_ERROR";

    public static final String INTERNAL_ERROR =
            "Ocurrió un error interno en el servidor";

    public static final String INTERNAL_ERROR_CODE =
            "INTERNAL_SERVER_ERROR";
}
