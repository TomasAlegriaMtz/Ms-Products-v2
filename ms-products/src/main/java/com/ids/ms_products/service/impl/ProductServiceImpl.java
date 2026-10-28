package com.ids.ms_products.service.impl;

import com.ids.ms_products.constants.ErrorMessages;
import com.ids.ms_products.dto.*;
import com.ids.ms_products.exceptions.*;
import com.ids.ms_products.entity.Category;
import com.ids.ms_products.entity.PriceHistory;
import com.ids.ms_products.entity.Product;
import com.ids.ms_products.repository.CategoryRepository;
import com.ids.ms_products.repository.PriceHistoryRepository;
import com.ids.ms_products.repository.ProductRepository;
import com.ids.ms_products.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService{
    @Autowired 
    private ProductRepository repository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    private static final int PAGE_SIZE = 5;

    //A decidir
    @Override
    public List<ProductResponseDTO> getInactiveProducts() {
        List<Product> inactives = repository.findByActiveFalse();
        if (inactives.isEmpty()) {
            throw new NoProductsAvailableException("No hay productos inactivos en el catálogo");
        }
        return listResponseDTO(inactives);
    }

    @Override
    @Transactional
    public ProductResponseDTO saveProduct(ProductRegisterRequest dto) {

        if (dto.getName() == null || dto.getName().isBlank()){
            throw new ProductNotValidException(String.format(ErrorMessages.PRODUCT_NAME_INVALID));
        }

        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductNotValidException(String.format(ErrorMessages.PRODUCT_PRICE_INVALID));
        }

        validarReglaStock(dto.getStock());

        String productName = dto.getName().trim();
        if (repository.existsByNameIgnoreCase(productName)) {
            throw new ProductAlreadyExistsException(
                    "Ya existe un producto registrado con el nombre " + productName);
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ProductException(String.format(ErrorMessages.CATEGORY_INVALID)));

        Product product = new Product();

        product.setName(productName);
        product.setPrice(dto.getPrice());
        product.setDescription(dto.getDescription());
        product.setStock(dto.getStock());
        product.setCategory(category);


        product = repository.save(product);
        return responseDTO(product);
    }


    @Override
    public ProductResponseDTO getProductDetailById(Integer id) {
        Product p = findProductById(id);
        return responseDTO(p);
    }

    @Override
    public ProductResponseDTO updateProduct(Integer id, ProductRequestDTO dto) {
        Product product = findProductById(id);



        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ProductNotFoundException(
                    String.format(
                        ErrorMessages.PRODUCT_CATEGORY_NOT_FOUND
                    )
                ));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(category);
        product.setStock(dto.getStock());

        product = repository.save(product);

        return responseDTO(product);
    }

    //Que pueda activar un producto tambien. Alfredo
    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        Product product = findProductById(id);
        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ProductAlreadyInactiveException("El producto ya fue retirado del catálogo");
        }
        product.setActive(false);
        repository.save(product);
    }

    @Override
    public Integer getStockByProductId(Integer id) {
        Product product = findProductById(id);
        return product.getStock();
    }

    private void validarReglaStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new ProductException("El stock no puede ser negativo");
        }

        if (stock <= 100) {
             throw new ProductException("El stock mínimo requerido es de 100 unidades");
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BulkPriceUpdateResponse bulkUpdatePrices(BulkPriceUpdateRequest request) {
        if (request == null || request.getUpdates() == null || request.getUpdates().isEmpty()) {
            throw new ProductException("El lote de actualizaciones no puede estar vacío");
        }

        List<PriceUpdateItem> updates = request.getUpdates();
        validarTamanoLote(updates);

        Set<Integer> uniqueIds = new HashSet<>();
        List<Integer> duplicateIds = new ArrayList<>();
        for (PriceUpdateItem item : updates) {
            if (item.getProductId() != null && !uniqueIds.add(item.getProductId())) {
                duplicateIds.add(item.getProductId());
            }
        }
        if (!duplicateIds.isEmpty()) {
            throw new ProductException("No se permiten IDs duplicados dentro de la misma solicitud: " + duplicateIds);
        }

        for (PriceUpdateItem item : updates) {
            if (item.getProductId() == null) {
                throw new ProductException("El ID del producto no puede ser nulo");
            }
            if (item.getNewPrice() == null || item.getNewPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ProductException("Todos los precios deben ser mayores que cero. Precio inválido para producto ID: " + item.getProductId());
            }
        }

        List<Integer> ids = updates.stream().map(PriceUpdateItem::getProductId).toList();
        List<Product> products = repository.findAllById(ids);

        Set<Integer> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        List<Integer> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();

        if (!missingIds.isEmpty()) {
            throw new ProductNotFoundException("El lote fue rechazado porque los siguientes productos no existen: " + missingIds);
        }

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        String currentAdmin = getCurrentUsername();
        List<ProductPriceUpdatedDTO> updatedDtos = new ArrayList<>();
        List<PriceHistory> historyRecords = new ArrayList<>();

        for (PriceUpdateItem item : updates) {
            Product product = productMap.get(item.getProductId());
            BigDecimal oldPrice = product.getPrice();
            BigDecimal newPrice = item.getNewPrice();

            product.setPrice(newPrice);
            repository.save(product);

            PriceHistory history = new PriceHistory(product, oldPrice, newPrice, currentAdmin);
            historyRecords.add(history);

            updatedDtos.add(new ProductPriceUpdatedDTO(
                    product.getId(),
                    product.getName(),
                    oldPrice,
                    newPrice
            ));
        }

        priceHistoryRepository.saveAll(historyRecords);

        return new BulkPriceUpdateResponse(
                "Lote de precios actualizado con éxito.",
                updatedDtos.size(),
                updatedDtos
        );
    }

    private void validarTamanoLote(List<PriceUpdateItem> updates) {
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "ADMIN";
    }

    public Page<ProductResponseDTO> catalogProducts(ProductRequestPagDTO dto){
        ProductRequestPagDTO req;
        if(dto != null){
            req = dto;
        } else {
            req = new ProductRequestPagDTO();
        }

        if(req.getMinPrice() != null && req.getMaxPrice() != null && req.getMinPrice().compareTo(req.getMaxPrice()) > 0){
            throw new InvalidPriceRangeException("El precio minimo no puede ser mayor al precio maximo");
        }

        int page = req.getPage() != null ? req.getPage() : 0;
        if(page < 0){
            throw new ProductException("La pagina no puede ser negativa");
        }

        int size = req.getSize() != null && req.getSize() <= PAGE_SIZE ? req.getSize() : PAGE_SIZE;
        if(size < 1){
            throw new ProductException("El tamaño de página debe ser al menos 1");
        }

        SortPageType sort = req.getSort() != null ? req.getSort() : SortPageType.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.valueOf(String.valueOf(sort)), "price"));

        String name = (req.getName() != null && !req.getName().isBlank()) ? req.getName() : null;

        return repository.searchCatalog(name, req.getCategoryId(), req.getMinPrice(), req.getMaxPrice(), pageable).map(product -> responseDTO(product));
    }

    private ProductResponseDTO responseDTO(Product product){
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getActive(),
                product.getCategory().getId()
        );
    }
    private List<ProductResponseDTO> listResponseDTO(List<Product> products){
        return products.stream().map(this::responseDTO).toList();
    }
    private Product findProductById(Integer id){
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format(ErrorMessages.PRODUCT_NOT_FOUND, id)));
    }
}
