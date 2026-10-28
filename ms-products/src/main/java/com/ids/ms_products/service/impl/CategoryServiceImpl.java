package com.ids.ms_products.service.impl;

import com.ids.ms_products.dto.CategoryResponseDTO;
import com.ids.ms_products.repository.CategoryRepository;
import com.ids.ms_products.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Page<CategoryResponseDTO> obtenerCategorias(Pageable pageable) {
        return categoryRepository.findByActiveTrue(pageable)
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName()
                ));
    }
}