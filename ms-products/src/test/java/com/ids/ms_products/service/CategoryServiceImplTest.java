package com.ids.ms_products.service;

import com.ids.ms_products.dto.CategoryResponseDTO;
import com.ids.ms_products.entity.Category;
import com.ids.ms_products.repository.CategoryRepository;
import com.ids.ms_products.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void obtenerCategoriasRetornaMapeoCorrecto() {
        Category cat = new Category();
        cat.setId(1);
        cat.setName("Hogar");

        PageRequest pageable = PageRequest.of(0, 5);
        Page<Category> categoryPage = new PageImpl<>(List.of(cat));

        when(categoryRepository.findByActiveTrue(any(Pageable.class))).thenReturn(categoryPage);

        Page<CategoryResponseDTO> res = categoryService.obtenerCategorias(pageable);
        assertEquals(1, res.getContent().size());
        assertEquals("Hogar", res.getContent().get(0).getName());
    }
}