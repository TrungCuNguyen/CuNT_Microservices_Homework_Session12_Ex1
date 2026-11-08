package com.example.productservice.service.impl;

import com.example.productservice.config.RedisConfig;
import com.example.productservice.dto.request.ProductRequestDTO;
import com.example.productservice.dto.response.ProductResponseDTO;
import com.example.productservice.entity.Product;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * Lấy chi tiết sản phẩm theo ID áp dụng Cache-Aside pattern.
     * - Cache Hit: Lấy trực tiếp từ Redis (< 10ms), method này không được thực thi.
     * - Cache Miss: Gọi xuống Database (~200ms), kết quả được lưu tự động vào Redis với key là productId.
     */
    @Override
    @Cacheable(value = RedisConfig.PRODUCT_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        log.info(">>> [DATABASE QUERY] Cache Miss! Truy vấn SQL Database cho Product ID: {}", id);
        
        // Mô phỏng độ trễ truy vấn Database phức tạp (~200ms) theo yêu cầu bài tập
        simulateDatabaseLatency(200);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        log.info(">>> [DATABASE QUERY] Lấy thành công dữ liệu từ DB cho ID: {}. Chuẩn bị lưu vào Redis Cache.", id);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        Product product = Product.builder()
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .imageUrl(requestDTO.getImageUrl())
                .price(requestDTO.getPrice())
                .stockQuantity(requestDTO.getStockQuantity())
                .build();

        Product saved = productRepository.save(product);
        log.info("Đã tạo mới sản phẩm ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Cập nhật thông tin sản phẩm và tự động xóa Cache (@CacheEvict)
     * Đảm bảo dữ liệu trong Redis luôn đồng nhất khi quản trị viên cập nhật.
     */
    @Override
    @CacheEvict(value = RedisConfig.PRODUCT_CACHE, key = "#id")
    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        log.info(">>> [CACHE EVICT] Bắt đầu cập nhật sản phẩm ID: {} và xóa cache tương ứng", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        product.setName(requestDTO.getName());
        product.setDescription(requestDTO.getDescription());
        product.setImageUrl(requestDTO.getImageUrl());
        product.setPrice(requestDTO.getPrice());
        product.setStockQuantity(requestDTO.getStockQuantity());

        Product updated = productRepository.save(product);
        log.info(">>> [CACHE EVICT] Cập nhật thành công DB và đã xóa cache Redis cho ID: {}", id);
        return mapToResponse(updated);
    }

    /**
     * Xóa sản phẩm và xóa Cache tương ứng trong Redis
     */
    @Override
    @CacheEvict(value = RedisConfig.PRODUCT_CACHE, key = "#id")
    @Transactional
    public void deleteProduct(Long id) {
        log.info(">>> [CACHE EVICT] Xóa sản phẩm ID: {} và xóa cache tương ứng", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProductResponseDTO mapToResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .build();
    }

    private void simulateDatabaseLatency(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

