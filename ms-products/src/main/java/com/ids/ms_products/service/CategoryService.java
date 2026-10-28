package com.ids.ms_products.service;

import com.ids.ms_products.dto.CategoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    Page<CategoryResponseDTO> obtenerCategorias(Pageable pageable);

}