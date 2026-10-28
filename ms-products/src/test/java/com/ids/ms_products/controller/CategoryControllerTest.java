package com.ids.ms_products.controller;

import com.ids.ms_products.dto.CategoryResponseDTO;
import com.ids.ms_products.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    void obtenerCategoriasRetornaPaginaValida() {
        CategoryResponseDTO dto = new CategoryResponseDTO(1, "Electrónica");
        Page<CategoryResponseDTO> page = new PageImpl<>(List.of(dto));

        when(categoryService.obtenerCategorias(any(Pageable.class))).thenReturn(page);

        Page<CategoryResponseDTO> resultado = categoryController.obtenerCategorias(0, 10, "name,asc");
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
    }

    @Test
    void obtenerCategoriasLanzaExcepcionConOrdenInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                categoryController.obtenerCategorias(0, 10, "invalido"));
        assertThrows(IllegalArgumentException.class, () ->
                categoryController.obtenerCategorias(0, 10, "name,desconhecido"));
    }

    @Test
    void obtenerCategoriasConOrdenDesc() {
        CategoryResponseDTO dto = new CategoryResponseDTO(1, "Electrónica");
        Page<CategoryResponseDTO> page = new PageImpl<>(List.of(dto));

        when(categoryService.obtenerCategorias(any(Pageable.class))).thenReturn(page);

        Page<CategoryResponseDTO> resultado = categoryController.obtenerCategorias(0, 10, "name,desc");
        assertNotNull(resultado);
    }

    @Test
    void obtenerCategoriasLanzaExcepcionConCampoDistintoDeName() {
        assertThrows(IllegalArgumentException.class, () ->
                categoryController.obtenerCategorias(0, 10, "price,asc"));
    }
}