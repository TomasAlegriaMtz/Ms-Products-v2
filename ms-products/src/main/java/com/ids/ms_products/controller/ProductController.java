package com.ids.ms_products.controller;

import com.ids.ms_products.dto.*;
import com.ids.ms_products.exceptions.ProductException;
import com.ids.ms_products.service.ProductService;
import com.ids.ms_products.service.impl.ProductImportService;
import com.ids.ms_products.service.impl.PromotionService;
import com.ids.ms_products.service.impl.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Operaciones del catálogo, inventario y administración de productos")
public class ProductController {
    private final ProductService service;
    private final StockMovementService stockMovementService;
    private final ProductImportService importService;
    private final PromotionService promotionService;

    public ProductController(StockMovementService stockMovementService, ProductService service, ProductImportService importService, PromotionService promotionService) {
        this.stockMovementService = stockMovementService;
        this.service = service;
        this.importService = importService;
        this.promotionService = promotionService;
    }

    // 1. REGISTRAR UN PRODUCTO (Tomas Alegria)
    @Operation(
            summary = "Registrar un nuevo producto",
            description = "Crea un producto en el catálogo. Requiere rol ADMIN y valida precio, stock y categoría obligatorios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ProductResponseDTO> saveProduct(
            @Valid @RequestBody ProductRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveProduct(request));
    }

    // 2. REGISTRO MASIVO VÍA ARCHIVO CSV
    @Operation(
            summary = "Registro masivo de productos vía archivo CSV",
            description = "Crea múltiples productos a partir de un archivo .csv. Requiere rol CLIENT o ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo procesado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Archivo inválido o formato de encabezados incorrecto"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<ImportResultDTO> importProducts(
            @Parameter(description = "Archivo .csv con los productos a procesar")
            @RequestParam("file") MultipartFile file) throws IOException {
        importService.validarArchivo(file);
        importService.validarEncabezados(file);
        return ResponseEntity.ok(importService.procesarCSV(file));
    }

    // 3. CONSULTAR EL CATÁLOGO CON FILTROS Y PAGINACIÓN (Tomas Alegria)
    @Operation(
            summary = "Consultar el catálogo con filtros y paginación",
            description = "Retorna una página de productos filtrados por nombre, categoría y/o rango de precios. Accesible para CLIENT y ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo consultado con éxito"),
            @ApiResponse(responseCode = "400", description = "Rango de precios inválido (precio mínimo mayor al máximo)"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(@ModelAttribute ProductRequestPagDTO request){
        return ResponseEntity.ok(service.catalogProducts(request));
    }

    // 4. CONSULTAR DETALLE DE UN PRODUCTO (Alfredo)
    @Operation(
            summary = "Consultar el detalle de un producto",
            description = "Retorna la información completa de un producto a partir de su ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{id}/detail")
    public ResponseEntity<ProductResponseDTO> getProductDetailById(
            @Parameter(description = "ID único del producto", example = "1")
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.getProductDetailById(id));
    }

    // 5. ACTUALIZAR PRODUCTO (Alfredo)
    @Operation(
            summary = "Actualizar nombre, descripción, precio u otros datos",
            description = "Actualiza los datos permitidos de un producto existente. Requiere rol ADMIN (verificado por Spring Security y por header)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: Se requieren permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @Parameter(description = "ID del producto a actualizar", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequestDTO request,
            @Parameter(description = "Rol enviado en el header (debe ser admin)", example = "admin")
            @RequestHeader(value = "rol", required = false) String rol){
        if(!"admin".equalsIgnoreCase(rol)){
            throw new ProductException("Acceso denegado: Se requieren permisos de administrador");
        }
        return ResponseEntity.ok(service.updateProduct(id, request));
    }

    // 6. AJUSTAR EXISTENCIAS DE STOCK (Alfredo)
    @Operation(
            summary = "Ajustar existencias bajo reglas controladas",
            description = "Registra un ajuste de stock (entrada, salida, corrección) para un producto. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ajuste de stock registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Cantidad o tipo de ajuste inválido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PatchMapping("/{id}/stock-adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockMovementResponse> adjustStock(
            @Parameter(description = "ID del producto a ajustar stock", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.adjustStock(id, request, authentication.getName()));
    }

    // 7. CONSULTAR MOVIMIENTOS DE STOCK
    @Operation(
            summary = "Consultar historial de movimientos de inventario",
            description = "Retorna el historial de movimientos de stock asociados al producto especificado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido con éxito"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{id}/stock-movements")
    public ResponseEntity<List<StockMovementResponse>> getStockMovements(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Integer id) {
        return ResponseEntity.ok(stockMovementService.getStockMovements(id));
    }

    // 8. DESACTIVAR PRODUCTO (Edson)
    @Operation(
            summary = "Activar o desactivar producto del catálogo",
            description = "Retira un producto del catálogo activo mediante su ID recibido en el header. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto retirado del catálogo con éxito"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @Parameter(description = "ID del producto a dar de baja", example = "1")
            @RequestHeader("id") Integer id) {
        service.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Producto retirado del catálogo con éxito."));
    }

    // 9. CONSULTAR STOCK DISPONIBLE (Edson)
    @Operation(
            summary = "Consultar unidades suficientes de stock",
            description = "Consulta la cantidad de existencias disponibles para la venta de un producto. Accesible para CLIENT y ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock consultado con éxito"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('CLIENT','ADMIN')")
    public ResponseEntity<Map<String, Object>> getProductStock(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Integer id) {
        Integer stock = service.getStockByProductId(id);
        return ResponseEntity.ok(Map.of(
                "productId", id,
                "stock", stock
        ));
    }

    // 10. ACTUALIZACIÓN MASIVA DE PRECIOS
    @Operation(
            summary = "Actualización masiva de precios",
            description = "Permite modificar precios en bloque bajo criterios definidos. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Precios actualizados con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    @PatchMapping("/prices/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkPriceUpdateResponse> bulkUpdatePrices(
            @Valid @RequestBody BulkPriceUpdateRequest request) {
        return ResponseEntity.ok(service.bulkUpdatePrices(request));
    }

    // 11. CREAR PROMOCIÓN / DESCUENTO
    @Operation(
            summary = "Crear promoción o descuento",
            description = "Registra una nueva promoción en el sistema. Requiere rol ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promoción creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de promoción inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    @PostMapping("/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
    }

    // 12. CONSULTAR PROMOCIONES ACTIVAS
    @Operation(
            summary = "Consultar promociones actualmente activas",
            description = "Retorna el listado de promociones que se encuentran vigentes en este momento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de promociones activas obtenido con éxito")
    })
    @GetMapping("/promotions/active")
    public ResponseEntity<List<PromotionResponse>> getCurrentlyActive() {
        return ResponseEntity.ok(promotionService.getCurrentlyActive());
    }
}
