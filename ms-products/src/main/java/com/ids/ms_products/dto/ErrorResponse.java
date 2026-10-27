package com.ids.ms_products.dto;

import java.time.Instant;

public record ErrorResponse(
    Instant timestamp, 
    int status, 
    String code, 
    String message, 
    String path
) {}
