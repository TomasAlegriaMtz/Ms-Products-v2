package com.ids.ms_products.service;

import com.ids.ms_products.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IProductService {
    ProductResponseDTO saveProduct(ProductRegisterRequest dto);
    List<ProductResponseDTO> getInactiveProducts();
    ProductResponseDTO getProductById(Integer id);
    ProductResponseDTO updateProduct(Integer id, ProductRequestDTO dto);
    void deleteProduct(Integer id);
    Integer getStockByProductId(Integer id);
    BulkPriceUpdateResponse bulkUpdatePrices(BulkPriceUpdateRequest request);
    Page<ProductResponseDTO> catalogProducts(ProductRequestPagDTO dto);
}
