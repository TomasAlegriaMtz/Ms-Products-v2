package com.ids.ms_products.service;

import com.ids.ms_products.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductResponseDTO saveProduct(ProductRegisterRequest dto);
    List<ProductResponseDTO> getInactiveProducts();
    ProductResponseDTO getProductDetailById(Integer id);
    ProductResponseDTO updateProduct(Integer id, ProductRequestDTO dto);
    void deleteProduct(Integer id);
    Integer getStockByProductId(Integer id);
    BulkPriceUpdateResponse bulkUpdatePrices(BulkPriceUpdateRequest request);
    Page<ProductResponseDTO> catalogProducts(ProductRequestPagDTO dto);
    ProductAvailabilityResponseDTO getProductAvailability(Integer id, Integer quantity);
    // Activar o desactivar un producto mediante su ID. Edson
    void updateProductStatus(Integer id, Boolean active);
}
