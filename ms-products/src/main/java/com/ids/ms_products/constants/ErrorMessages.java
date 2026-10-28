package com.ids.ms_products.constants;

public final class ErrorMessages {
    private ErrorMessages() {
        // Private constructor to prevent instantiation
    }

    public static final String PRODUCT_NOT_FOUND = 
        "No se encontró el producto con id %d";

    public static final String PRODUCT_NOT_FOUND_CODE =
        "PRODUCT_NOT_FOUND";

    public static final String PRODUCT_NAME_INVALID =
        "El nombre del producto es obligatorio";

    public static final String PRODUCT_NAME_INVALID_CODE =
        "PRODUCT_NAME_INVALID";

    public static final String PRODUCT_PRICE_INVALID =
        "El precio debe ser mayor a cero";

    public static final String PRODUCT_PRICE_INVALID_CODE =
        "PRODUCT_PRICE_INVALID";

    public static final String PRODUCT_ID_INVALID =
        "El ID del producto debe ser mayor que cero";

    public static final String PRODUCT_ID_INVALID_CODE =
        "PRODUCT_ID_INVALID";

    public static final String CATEGORY_INVALID =
        "La categoría no existe";

    public static final String CATEGORY_INVALID_CODE =
        "CATEGORY_INVALID";

    public static final String PRODUCT_CATEGORY_NOT_FOUND =
        "La categoría del producto no existe";

    public static final String PRODUCT_CATEGORY_NOT_FOUND_CODE =
        "PRODUCT_CATEGORY_NOT_FOUND";

    public static final String ACCESS_DENIED = 
        "Acceso denegado: Se requieren permisos de administrador";

    public static final String ACCESS_DENIED_CODE = 
        "ACCESS_DENIED";

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
