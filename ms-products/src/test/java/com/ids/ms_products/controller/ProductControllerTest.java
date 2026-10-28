package com.ids.ms_products.controller;

import com.ids.ms_products.dto.*;
import com.ids.ms_products.exceptions.GlobalExceptionHandler;
import com.ids.ms_products.exceptions.ProductAlreadyInactiveException;
import com.ids.ms_products.exceptions.ProductNotFoundException;
import com.ids.ms_products.service.ProductService;
import com.ids.ms_products.service.impl.ProductImportService;
import com.ids.ms_products.service.impl.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService service;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private ProductImportService importService;

    @InjectMocks
    private ProductController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/status debe retornar 200 OK")
    void deleteProduct_Success() throws Exception {
        Integer productId = 1;
        when(service.updateProductStatus(productId, null))
                .thenReturn("Producto retirado del catálogo con éxito.");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/products/{id}/status", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Producto retirado del catálogo con éxito."));
        verify(service, times(1)).updateProductStatus(productId, null);
    }
    @Test
    @DisplayName("PATCH /api/products/{id}/status debe retornar 404 NOT FOUND")
    void deleteProduct_NotFound() throws Exception {
        Integer productId = 99;
        doThrow(new ProductNotFoundException("No se encontró el producto con id " + productId))
                .when(service).updateProductStatus(productId, null);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/products/{id}/status", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el producto con id " + productId));
        verify(service, times(1)).updateProductStatus(productId, null);
    }
    @Test
    @DisplayName("PATCH /api/products/{id}/status debe retornar 409 CONFLICT")
    void deleteProduct_AlreadyInactive() throws Exception {
        Integer productId = 1;
        doThrow(new ProductAlreadyInactiveException("El producto ya fue retirado del catálogo"))
                .when(service).updateProductStatus(productId, null);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/products/{id}/status", productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El producto ya fue retirado del catálogo"));
        verify(service, times(1)).updateProductStatus(productId, null);
    }

    @Test
    @DisplayName("Pruebas directas de endpoints de consulta y escritura del controller")
    void delegatesProductEndpointsCorrectly() throws Exception {
        ProductResponseDTO product = new ProductResponseDTO(
                1, "Producto", "Descripción", new BigDecimal("10.00"), 150, true, 2);
        Page<ProductResponseDTO> page = new PageImpl<>(List.of(product));
        ProductRequestPagDTO pageRequest = new ProductRequestPagDTO();

        when(service.catalogProducts(pageRequest)).thenReturn(page);
        assertSame(page, controller.getProducts(pageRequest).getBody());

        when(service.getProductDetailById(1)).thenReturn(product);
        assertSame(product, controller.getProductDetailById(1).getBody());

        when(service.saveProduct(any(ProductRegisterRequest.class))).thenReturn(product);
        assertSame(product, controller.saveProduct(new ProductRegisterRequest()).getBody());

        when(service.updateProduct(eq(1), any(ProductRequestDTO.class))).thenReturn(product);
        assertSame(product, controller.updateProduct(1, new ProductRequestDTO()).getBody());

        when(service.getStockByProductId(1)).thenReturn(150);
        assertEquals(150, controller.getProductStock(1).getBody().get("stock"));

        BulkPriceUpdateResponse bulkResponse = new BulkPriceUpdateResponse("ok", 1, List.of());
        when(service.bulkUpdatePrices(any(BulkPriceUpdateRequest.class))).thenReturn(bulkResponse);
        assertSame(bulkResponse, controller.bulkUpdatePrices(new BulkPriceUpdateRequest()).getBody());

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());
        ImportResultDTO importResult = new ImportResultDTO();
        importResult.setProcesados(1);
        importResult.setAceptados(1);
        when(importService.procesarCSV(file)).thenReturn(importResult);
        assertSame(importResult, controller.importProducts(file).getBody());

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin@demo.com");
        StockMovementResponse stockResponse = new StockMovementResponse(
                1L,
                1,
                10,
                "ENTRADA",
                "Ajuste manual",
                1L,
                "admin@demo.com",
                java.time.LocalDateTime.now(),
                100
        );
        when(stockMovementService.adjustStock(eq(1), any(StockAdjustmentRequest.class), eq("admin@demo.com"))).thenReturn(stockResponse);
        assertSame(stockResponse, controller.adjustStock(1, new StockAdjustmentRequest(), auth).getBody());

        when(stockMovementService.getStockMovements(1)).thenReturn(List.of(stockResponse));
        assertSame(1, controller.getStockMovements(1).getBody().size());
    }
}