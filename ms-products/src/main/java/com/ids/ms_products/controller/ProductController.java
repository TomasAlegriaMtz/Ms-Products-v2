package com.ids.ms_products.controller;

import com.ids.ms_products.dto.*;
import com.ids.ms_products.exceptions.ProductException;
import com.ids.ms_products.service.ProductService;
import com.ids.ms_products.service.impl.ProductImportService;
import com.ids.ms_products.service.impl.StockMovementService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import static com.ids.ms_products.constants.ErrorMessages.ACCESS_DENIED;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    private final StockMovementService stockMovementService;
    private final ProductImportService importService;

    public ProductController(StockMovementService stockMovementService, ProductService service, ProductImportService importService) {
        this.stockMovementService = stockMovementService;
        this.service = service;
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ProductResponseDTO> saveProduct(
            @Valid @RequestBody ProductRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveProduct(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ImportResultDTO> importProducts(
            @RequestParam("file") MultipartFile file) throws IOException {
        importService.validarArchivo(file);
        importService.validarEncabezados(file);
        return ResponseEntity.ok(importService.procesarCSV(file));
    }

    //Consultar el catálogo con filtros y paginación. Tomás
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(@RequestBody(required = false) ProductRequestPagDTO request){
        return ResponseEntity.ok(service.catalogProducts(request));
    }

    //Consultar el detalle de un producto. Alfredo
    @GetMapping("/{id}/detail")
    //@PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ProductResponseDTO> getProductDetailById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getProductDetailById(id));
    }

    //Actualizar nombre, descripción, precio u otros datos permitidos. Alfredo
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
        @PathVariable Integer id,
        @Valid @RequestBody ProductRequestDTO request) {
            
        return ResponseEntity.ok(service.updateProduct(id, request));
    }

    //Ajustar existencias bajo reglas controladas. Alfredo
    //Cambiar a Patch y la ruta a /{id}/stock
    @PatchMapping("/stock-adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockMovementResponse> adjustStock(
            @PathVariable Integer id,
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.adjustStock(id, request, authentication.getName()));
    }

    //Opcional. Agregar Page, size y sort ya que es un listado
    @GetMapping("/stock-movements")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(@PathVariable Integer id) {
        return ResponseEntity.ok(stockMovementService.getStockMovements(id));
    }


    //Activar o desactivar un producto, ahorita solo desactiva, con /{id}/status. Edson
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProduct(@RequestHeader("id") Integer id) {
        service.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Producto retirado del catálogo con éxito."));
    }

    //Consultar si hay unidades suficientes para una cantidad solicitada. Falta recibir la cantidad requerida. Edson
    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductStock(@PathVariable Integer id) {
        Integer stock = service.getStockByProductId(id);
        return ResponseEntity.ok(Map.of(
                "productId", id,
                "stock", stock
        ));
    }

    //Actualizar precios. opcional
    @PatchMapping("/prices/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkPriceUpdateResponse> bulkUpdatePrices(
            @Valid @RequestBody BulkPriceUpdateRequest request) {
        return ResponseEntity.ok(service.bulkUpdatePrices(request));
    }



    // Consultar disponibilidad de un producto. Edson
    @GetMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ProductAvailabilityResponseDTO> getProductAvailability(
            @PathVariable @Positive Integer id,
            @RequestParam @Positive Integer quantity) {

        return ResponseEntity.ok(service.getProductAvailability(id, quantity));
    }


}
