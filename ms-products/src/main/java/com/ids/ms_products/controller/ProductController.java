package com.ids.ms_products.controller;

import com.ids.ms_products.dto.*;
import com.ids.ms_products.service.ProductService;
import com.ids.ms_products.service.impl.ProductImportService;
import com.ids.ms_products.service.impl.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import io.swagger.v3.oas.annotations.Parameter;

import java.io.IOException;
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

    @Operation(
            summary = "Registrar un nuevo producto",
            description = "Crea un producto en el catálogo. Requiere rol ADMIN y valida precio, stock y categoría obligatorios."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ProductResponseDTO> saveProduct(
            @Valid @RequestBody ProductRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveProduct(request));
    }

    @Operation(
            summary = "Registro masivo de productos vía archivo CSV",
            description = "Crea múltiples productos a partir de un archivo .csv. Requiere rol CLIENT o ADMIN."
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ImportResultDTO> importProducts(
            @Parameter(description = "Archivo .csv con los productos a procesar")
            @RequestParam("file") MultipartFile file) throws IOException {
        importService.validarArchivo(file);
        importService.validarEncabezados(file);
        return ResponseEntity.ok(importService.procesarCSV(file));
    }

    @Operation(
            summary = "Consultar el catálogo con filtros y paginación",
            description = "Retorna una página de productos filtrados por nombre, categoría y/o rango de precios. Accesible para CLIENT y ADMIN."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(@ModelAttribute ProductRequestPagDTO request){
        return ResponseEntity.ok(service.catalogProducts(request));
    }


    //Consultar el detalle de un producto. Alfredo
    @Operation(
            summary = "Consultar el detalle de un producto",
            description = "Retorna la información completa de un producto a partir de su ID."
    )
    @GetMapping("/{id}/detail")
    //@PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ProductResponseDTO> getProductDetailById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getProductDetailById(id));
    }

    //Actualizar nombre, descripción, precio u otros datos permitidos. Alfredo
    @Operation(
            summary = "Actualizar nombre, descripción, precio u otros datos",
            description = "Actualiza los datos permitidos de un producto existente. Requiere rol ADMIN (verificado por Spring Security y por header)."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequestDTO request) {

        return ResponseEntity.ok(service.updateProduct(id, request));
    }

    //Ajustar existencias bajo reglas controladas. Alfredo
    @Operation(
            summary = "Ajustar existencias bajo reglas controladas",
            description = "Registra un ajuste de stock (entrada, salida, corrección) para un producto. Requiere rol ADMIN."
    )
    //Cambiar a Patch y la ruta a /{id}/stock
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockMovementResponse> adjustStock(
            @PathVariable Integer id,
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.adjustStock(id, request, authentication.getName()));
    }

    //Opcional. Agregar Page, size y sort ya que es un listado
    @Operation(
            summary = "Consultar historial de movimientos de inventario",
            description = "Retorna el historial de movimientos de stock asociados al producto especificado."
    )
    @GetMapping("/stock-movements/{id}")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(@PathVariable Integer id) {
        return ResponseEntity.ok(stockMovementService.getStockMovements(id));
    }


    //Activar o desactivar un producto, ahorita solo desactiva, con /{id}/status. Edson
    @Operation(
            summary = "Activar o desactivar producto del catálogo",
            description = "Retira un producto del catálogo activo mediante su ID recibido en el header. Requiere rol ADMIN."
    )
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Integer id,
                                                             @RequestParam(required = false) Boolean active) {
        String msj = service.updateProductStatus(id, active);
        return ResponseEntity.ok(Map.of("message", msj));
    }

    //Consultar si hay unidades suficientes para una cantidad solicitada. Falta recibir la cantidad requerida. Edson
    @Operation(
            summary = "Consultar unidades suficientes de stock",
            description = "Consulta la cantidad de existencias disponibles para la venta de un producto. Accesible para CLIENT y ADMIN."
    )
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
    @Operation(
            summary = "Actualización masiva de precios",
            description = "Permite modificar precios en bloque bajo criterios definidos. Requiere rol ADMIN."
    )
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

    // Activar o desactivar un producto mediante su ID. Edson
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateProductStatus(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody ProductStatusRequestDTO request) {

        service.updateProductStatus(id, request.getActive());

        return ResponseEntity.ok(
                Map.of("message", "Estado del producto actualizado correctamente")
        );
    }


}
