package com.ids.ms_products.controller;

import com.ids.ms_products.dto.*;
import com.ids.ms_products.exceptions.GlobalExceptionHandler;
import com.ids.ms_products.exceptions.ProductAlreadyInactiveException;
import com.ids.ms_products.exceptions.ProductNotFoundException;
import com.ids.ms_products.service.IProductService;
import com.ids.ms_products.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService service;

    @InjectMocks
    private ProductController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("DELETE /api/products con header 'id' debe retornar 200 OK")
    void deleteProduct_Success() throws Exception {
        Integer productId = 1;
        doNothing().when(service).deleteProduct(productId);

        mockMvc.perform(delete("/api/products")
                        .header("id", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Producto retirado del catálogo con éxito."));

        verify(service, times(1)).deleteProduct(productId);
    }

    @Test
    @DisplayName("DELETE /api/products con header 'id' debe retornar 404 NOT FOUND")
    void deleteProduct_NotFound() throws Exception {
        Integer productId = 99;
        doThrow(new ProductNotFoundException("No se encontró el producto con id " + productId))
                .when(service).deleteProduct(productId);

        mockMvc.perform(delete("/api/products")
                        .header("id", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el producto con id " + productId));

        verify(service, times(1)).deleteProduct(productId);
    }

    @Test
    @DisplayName("DELETE /api/products con header 'id' debe retornar 409 CONFLICT")
    void deleteProduct_AlreadyInactive() throws Exception {
        Integer productId = 1;
        doThrow(new ProductAlreadyInactiveException("El producto ya fue retirado del catálogo"))
                .when(service).deleteProduct(productId);

        mockMvc.perform(delete("/api/products")
                        .header("id", productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El producto ya fue retirado del catálogo"));

        verify(service, times(1)).deleteProduct(productId);
    }

    @Test
    void delegatesProductReadAndWriteEndpoints() {
        ProductResponseDTO product = new ProductResponseDTO(
                1, "Producto", "Descripción", new BigDecimal("10.00"), 150, true, 2);
        Page<ProductResponseDTO> page = new PageImpl<>(List.of(product));
        ProductRequestPagDTO pageRequest = new ProductRequestPagDTO();
        when(service.catalogProducts(pageRequest)).thenReturn(page);
        assertSame(page, controller.getProducts(pageRequest).getBody());

        when(service.getInactiveProducts()).thenReturn(List.of(product));
        assertEquals(List.of(product), controller.getInactiveProducts().getBody());
        when(service.getProductById(1)).thenReturn(product);
        assertSame(product, controller.getProductById(1).getBody());
        when(service.saveProduct(any(ProductRegisterRequest.class))).thenReturn(product);
        assertSame(product, controller.saveProduct(new ProductRegisterRequest()).getBody());
        when(service.getStockByProductId(1)).thenReturn(150);
        assertEquals(150, controller.getProductStock(1).getBody().get("stock"));
        BulkPriceUpdateResponse bulkResponse = new BulkPriceUpdateResponse("ok", 1, List.of());
        when(service.bulkUpdatePrices(any(BulkPriceUpdateRequest.class))).thenReturn(bulkResponse);
        assertSame(bulkResponse, controller.bulkUpdatePrices(new BulkPriceUpdateRequest()).getBody());
        when(service.updateProduct(eq(1), any(ProductRequestDTO.class))).thenReturn(product);
        assertSame(product, controller.updateProduct(1, new ProductRequestDTO(), "ADMIN").getBody());
        when(service.filterProducts(isNull(), isNull())).thenReturn(List.of(product));
        assertEquals(List.of(product), controller.filterProducts(null, "admin").getBody());
        ProductRequestDTO filter = new ProductRequestDTO();
        filter.setCategoryId(2);
        when(service.filterProducts(2, filter)).thenReturn(List.of(product));
        assertEquals(List.of(product), controller.filterProducts(filter, "ADMIN").getBody());
        when(service.searchProductsByName("Producto")).thenReturn(List.of(product));
        assertEquals(List.of(product), controller.searchByName("Producto").getBody());
    }

    @Test
    void deniesProductUpdateAndFilteringForNonAdminRoles() {
        assertThrows(com.ids.ms_products.exceptions.ProductException.class,
                () -> controller.updateProduct(1, new ProductRequestDTO(), null));
        assertThrows(com.ids.ms_products.exceptions.ProductException.class,
                () -> controller.updateProduct(1, new ProductRequestDTO(), "client"));
        assertThrows(com.ids.ms_products.exceptions.ProductException.class,
                () -> controller.filterProducts(null, null));
        assertThrows(com.ids.ms_products.exceptions.ProductException.class,
                () -> controller.filterProducts(new ProductRequestDTO(), "client"));
        verifyNoInteractions(service);
    }
}
