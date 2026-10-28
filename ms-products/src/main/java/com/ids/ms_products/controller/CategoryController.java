package com.ids.ms_products.controller;



import com.ids.ms_products.dto.CategoryResponseDTO;
import com.ids.ms_products.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController


@Validated

@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(
            summary = "Consultar las categorias disponibles",
            description = "Consulta las categorias disponibles y las trae en pages, con size y sort"
    )
    @GetMapping
    public Page<CategoryResponseDTO> obtenerCategorias(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name,asc") String sort) {

        String[] orden = sort.split(",");

        if (orden.length != 2 ||
                !orden[0].equals("name") ||
                !(orden[1].equalsIgnoreCase("asc") ||
                        orden[1].equalsIgnoreCase("desc"))) {
            throw new IllegalArgumentException("Ordenamiento inválido");
        }

        Sort.Direction direccion = Sort.Direction.fromString(orden[1]);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(direccion, orden[0])
        );

        return categoryService.obtenerCategorias(pageable);
    }

}