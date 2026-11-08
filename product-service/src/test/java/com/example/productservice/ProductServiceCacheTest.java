package com.example.productservice;

import com.example.productservice.config.RedisConfig;
import com.example.productservice.config.TestCacheConfig;
import com.example.productservice.dto.request.ProductRequestDTO;
import com.example.productservice.dto.response.ProductResponseDTO;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestCacheConfig.class)
class ProductServiceCacheTest {

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Clear cache trước mỗi test
        Objects.requireNonNull(cacheManager.getCache(RedisConfig.PRODUCT_CACHE)).clear();

        productRepository.deleteAll();
        testProduct = productRepository.save(Product.builder()
                .name("MacBook Pro M3")
                .description("Mô tả MacBook")
                .imageUrl("https://example.com/macbook.jpg")
                .price(new BigDecimal("45000000"))
                .stockQuantity(10)
                .build());
        reset(productRepository);
    }

    @Test
    @DisplayName("Lần 1 truy vấn DB (Cache Miss), Lần 2 lấy từ Cache (Cache Hit) không gọi DB")
    void testCacheAsidePattern_GetProductById() {
        Long productId = testProduct.getId();

        // Lần 1: Chưa có cache -> Phải gọi productRepository.findById()
        ProductResponseDTO firstCall = productService.getProductById(productId);
        assertNotNull(firstCall);
        assertEquals(testProduct.getName(), firstCall.getName());
        verify(productRepository, times(1)).findById(productId);

        // Lần 2: Đã có cache -> Lấy từ Cache, KHÔNG gọi thêm vào DB
        ProductResponseDTO secondCall = productService.getProductById(productId);
        assertNotNull(secondCall);
        assertEquals(testProduct.getName(), secondCall.getName());
        // Verify findById vẫn chỉ được gọi đúng 1 lần (từ lần gọi đầu)
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Khi updateProduct thì cache bị xóa (@CacheEvict), lần get tiếp theo phải query lại DB")
    void testCacheEvict_OnUpdateProduct() {
        Long productId = testProduct.getId();

        // 1. Gọi lần đầu để nạp vào cache
        productService.getProductById(productId);
        verify(productRepository, times(1)).findById(productId);

        // 2. Cập nhật sản phẩm -> Kích hoạt @CacheEvict
        ProductRequestDTO updateDTO = ProductRequestDTO.builder()
                .name("MacBook Pro M3 Max")
                .description("Mô tả mới")
                .imageUrl("https://example.com/new.jpg")
                .price(new BigDecimal("60000000"))
                .stockQuantity(15)
                .build();
        productService.updateProduct(productId, updateDTO);

        // 3. Gọi getProductById lần nữa -> Vì cache đã bị evict, phương thức phải gọi
        // lại DB
        ProductResponseDTO thirdCall = productService.getProductById(productId);
        assertEquals("MacBook Pro M3 Max", thirdCall.getName());

        // Tổng số lần gọi findById là 3 (1 lần đầu + 1 lần trong updateProduct + 1 lần
        // sau khi evict)
        verify(productRepository, times(3)).findById(productId);
    }
}
