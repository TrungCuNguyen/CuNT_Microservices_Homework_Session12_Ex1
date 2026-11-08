package com.example.productservice.config;

import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu mẫu sản phẩm vào Database...");
            List<Product> sampleProducts = List.of(
                    Product.builder()
                            .name("iPhone 16 Pro Max 256GB")
                            .description("Điện thoại thông minh cao cấp của Apple trang bị chip A18 Pro và camera nâng cấp.")
                            .imageUrl("https://cdn.example.com/images/iphone-16-pro-max.png")
                            .price(new BigDecimal("34990000"))
                            .stockQuantity(50)
                            .build(),
                    Product.builder()
                            .name("Samsung Galaxy S24 Ultra")
                            .description("Flagship Android với bút S-Pen, camera 200MP và tính năng Galaxy AI tiên tiến.")
                            .imageUrl("https://cdn.example.com/images/s24-ultra.png")
                            .price(new BigDecimal("29990000"))
                            .stockQuantity(40)
                            .build(),
                    Product.builder()
                            .name("MacBook Pro 14 M3 Pro")
                            .description("Laptop chuyên nghiệp dành cho lập trình viên và đồ họa với màn hình Liquid Retina XDR.")
                            .imageUrl("https://cdn.example.com/images/macbook-pro-14-m3.png")
                            .price(new BigDecimal("49990000"))
                            .stockQuantity(25)
                            .build()
            );
            productRepository.saveAll(sampleProducts);
            log.info("Đã tạo {} sản phẩm mẫu thành công!", sampleProducts.size());
        }
    }
}
