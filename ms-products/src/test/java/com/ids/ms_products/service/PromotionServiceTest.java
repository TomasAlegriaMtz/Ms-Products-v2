package com.ids.ms_products.service;

import com.ids.ms_products.dto.PromotionRequest;
import com.ids.ms_products.dto.PromotionResponse;
import com.ids.ms_products.entity.Product;
import com.ids.ms_products.entity.Promotion;
import com.ids.ms_products.exceptions.ProductNotFoundException;
import com.ids.ms_products.exceptions.PromotionConflictException;
import com.ids.ms_products.exceptions.PromotionException;
import com.ids.ms_products.exceptions.PromotionNotFoundException;
import com.ids.ms_products.repository.ProductRepository;
import com.ids.ms_products.repository.PromotionRepository;
import com.ids.ms_products.service.impl.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private ProductRepository productRepository;

    private Clock clock;
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("UTC"));
        promotionService = new PromotionService(promotionRepository, productRepository, clock);
    }

    private PromotionRequest createValidRequest() {
        PromotionRequest request = new PromotionRequest();
        request.setName("Descuento Buen Fin");
        request.setDiscountPercentage(new BigDecimal("15.00"));
        request.setStartsAt(LocalDateTime.of(2026, 10, 20, 0, 0));
        request.setEndsAt(LocalDateTime.of(2026, 10, 25, 23, 59));
        request.setProductIds(List.of(1, 2));
        request.setActive(true);
        return request;
    }

    private Product createProduct(Integer id, String name, BigDecimal price) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        p.setActive(true);
        return p;
    }

    @Test
    void createPromotionSuccessfully() {
        PromotionRequest request = createValidRequest();
        Product p1 = createProduct(1, "Prod 1", new BigDecimal("100.00"));
        Product p2 = createProduct(2, "Prod 2", new BigDecimal("200.00"));

        when(productRepository.findByIdForUpdate(1)).thenReturn(Optional.of(p1));
        when(productRepository.findByIdForUpdate(2)).thenReturn(Optional.of(p2));
        when(promotionRepository.findOverlappingPromotions(anyList(), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());

        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> {
            Promotion p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PromotionResponse response = promotionService.create(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Descuento Buen Fin", response.getName());
        assertEquals(2, response.getProducts().size());
        assertEquals(new BigDecimal("85.00"), response.getProducts().get(0).getFinalPrice());
    }

    @Test
    void createThrowsWhenValidationFails() {
        PromotionRequest req = createValidRequest();
        req.setStartsAt(LocalDateTime.of(2026, 10, 26, 0, 0));
        req.setEndsAt(LocalDateTime.of(2026, 10, 20, 0, 0));
        assertThrows(PromotionException.class, () -> promotionService.create(req));

        PromotionRequest req2 = createValidRequest();
        req2.setDiscountPercentage(new BigDecimal("60.00"));
        assertThrows(PromotionException.class, () -> promotionService.create(req2));

        PromotionRequest req3 = createValidRequest();
        req3.setDiscountPercentage(BigDecimal.ZERO);
        assertThrows(PromotionException.class, () -> promotionService.create(req3));

        PromotionRequest req4 = createValidRequest();
        req4.setName("  ");
        assertThrows(PromotionException.class, () -> promotionService.create(req4));

        PromotionRequest req5 = createValidRequest();
        req5.setProductIds(List.of());
        assertThrows(PromotionException.class, () -> promotionService.create(req5));

        PromotionRequest req6 = createValidRequest();
        req6.setProductIds(List.of(1, 1));
        assertThrows(PromotionException.class, () -> promotionService.create(req6));
    }

    @Test
    void createThrowsWhenProductNotFound() {
        PromotionRequest request = createValidRequest();
        when(productRepository.findByIdForUpdate(1)).thenReturn(Optional.of(createProduct(1, "P1", BigDecimal.TEN)));
        when(productRepository.findByIdForUpdate(2)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> promotionService.create(request));
    }

    @Test
    void createThrowsWhenOverlappingPromotionExists() {
        PromotionRequest request = createValidRequest();
        Product p1 = createProduct(1, "Prod 1", new BigDecimal("100.00"));
        Product p2 = createProduct(2, "Prod 2", new BigDecimal("200.00"));

        when(productRepository.findByIdForUpdate(1)).thenReturn(Optional.of(p1));
        when(productRepository.findByIdForUpdate(2)).thenReturn(Optional.of(p2));
        when(promotionRepository.findOverlappingPromotions(anyList(), any(), any(), isNull()))
                .thenReturn(List.of(new Promotion()));

        assertThrows(PromotionConflictException.class, () -> promotionService.create(request));
    }

    @Test
    void updatePromotionSuccessfully() {
        PromotionRequest request = createValidRequest();
        Product p1 = createProduct(1, "Prod 1", new BigDecimal("100.00"));
        Product p2 = createProduct(2, "Prod 2", new BigDecimal("200.00"));

        Promotion existing = new Promotion();
        existing.setId(5L);
        existing.setActive(true);

        when(promotionRepository.findLockedById(5L)).thenReturn(Optional.of(existing));
        when(productRepository.findByIdForUpdate(1)).thenReturn(Optional.of(p1));
        when(productRepository.findByIdForUpdate(2)).thenReturn(Optional.of(p2));
        when(promotionRepository.findOverlappingPromotions(anyList(), any(), any(), eq(5L)))
                .thenReturn(Collections.emptyList());
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(i -> i.getArgument(0));

        PromotionResponse response = promotionService.update(5L, request);
        assertNotNull(response);
        assertEquals(5L, response.getId());
    }

    @Test
    void updateThrowsWhenPromotionNotFound() {
        PromotionRequest request = createValidRequest();
        when(promotionRepository.findLockedById(99L)).thenReturn(Optional.empty());

        assertThrows(PromotionNotFoundException.class, () -> promotionService.update(99L, request));
    }

    @Test
    void getCurrentlyActiveReturnsOnlyActivePromotions() {
        LocalDateTime now = LocalDateTime.now(clock);
        Product p = createProduct(1, "P1", new BigDecimal("50.00"));

        Promotion promo = new Promotion();
        promo.setId(1L);
        promo.setName("Promo Activa");
        promo.setDiscountPercentage(new BigDecimal("10.00"));
        promo.setStartsAt(now.minusDays(1));
        promo.setEndsAt(now.plusDays(1));
        promo.setActive(true);
        promo.setProducts(Set.of(p));

        when(promotionRepository.findCurrentlyActive(any(LocalDateTime.class)))
                .thenReturn(List.of(promo));

        List<PromotionResponse> active = promotionService.getCurrentlyActive();
        assertEquals(1, active.size());
        assertEquals("Promo Activa", active.get(0).getName());
    }


}