package com.ids.ms_products.dto;

public class ProductAvailabilityResponseDTO {





    private Integer productId;
    private Boolean available;


    public ProductAvailabilityResponseDTO(Integer productId, Boolean available) {
        this.productId = productId;
        this.available = available;
    }

    public Integer getProductId() {
        return productId;
    }

    public Boolean getAvailable() {
        return available;
    }


}
