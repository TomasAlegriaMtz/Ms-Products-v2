package com.ids.ms_products.dto;



public class CategoryResponseDTO {

    private Integer id;
    private String name;

    public CategoryResponseDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}