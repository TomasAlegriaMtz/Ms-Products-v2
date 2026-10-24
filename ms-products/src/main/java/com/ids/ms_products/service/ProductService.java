package com.ids.ms_products.service;

import com.ids.ms_products.dto.ProductResponseDTO;
import com.ids.ms_products.dto.ProductRegisterRequest;

public interface ProductService {
    ProductResponseDTO saveProduct(ProductRegisterRequest dto);

    

}
