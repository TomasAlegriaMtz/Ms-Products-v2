package com.ids.ms_products.dto;

import jakarta.validation.constraints.NotNull;

public class ProductStatusRequestDTO {

    @NotNull(message = "El estado del producto es obligatorio")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}