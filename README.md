# Tối Ưu Hóa Hiển Thị Sản Phẩm Với Redis Cache (Product Service)

Dự án áp dụng mô hình **Cache-Aside Pattern** kết hợp giữa **Spring Cache** và **Redis** nhằm giảm tải truy vấn SQL xuống Database, tăng tốc độ phản hồi cho trang chi tiết sản phẩm.

---

## 1. Mục Tiêu & Yêu Cầu Kỹ Thuật

- **Cache-Aside Pattern**:
  - Khi người dùng xem chi tiết sản phẩm (`GET /api/v1/products/{id}`):
    - **Lần đầu (Cache Miss)**: Đọc từ Database (~200ms), tự động nạp kết quả vào Redis Cache.
    - **Các lần tiếp theo (Cache Hit)**: Lấy trực tiếp từ Redis (< 10ms), không chạm vào Database.
- **Redis Serialization**:
  - Key Serializer: `StringRedisSerializer` (dạng key dễ đọc như `products::1`).
  - Value Serializer: `GenericJackson2JsonRedisSerializer` (JSON format chuẩn, hỗ trợ `JavaTimeModule`).
- **TTL (Time-To-Live)**:
  - Thiết lập TTL là **30 phút** (`Duration.ofMinutes(30)`) để giải phóng bộ nhớ Redis cho các sản phẩm cũ ít truy cập.
- **Xóa Cache Tự Động (@CacheEvict)**:
  - Khi cập nhật thông tin sản phẩm (`PUT /api/v1/products/{id}`), cache tương ứng bị xóa tự động để tránh dirty read.
  - Khi xóa sản phẩm (`DELETE /api/v1/products/{id}`), cache cũng được dọn sạch.

---

## 2. Cấu Trúc Dự Án

```
ex_ss12_micro/
├── .gitignore
├── docker-compose.yml
├── settings.gradle
├── gradlew / gradlew.bat
├── README.md
└── product-service/
    ├── build.gradle
    └── src/
        ├── main/
        │   ├── java/com/example/productservice/
        │   │   ├── ProductServiceApplication.java
        │   │   ├── config/
        │   │   │   ├── RedisConfig.java          # Cấu hình CacheManager, TTL 30m, JSON Serializer
        │   │   │   └── DataInitializer.java      # Nạp sẵn sản phẩm mẫu khi khởi động
        │   │   ├── controller/
        │   │   │   └── ProductController.java    # API REST endpoints & đo thời gian response
        │   │   ├── dto/
        │   │   │   ├── request/ProductRequestDTO.java
        │   │   │   └── response/ProductResponseDTO.java
        │   │   │   └── response/ApiResponse.java
        │   │   ├── entity/
        │   │   │   └── Product.java              # Entity sản phẩm (name, description, imageUrl, price, stock)
        │   │   ├── exception/
        │   │   │   ├── GlobalExceptionHandler.java
        │   │   │   └── ResourceNotFoundException.java
        │   │   ├── repository/
        │   │   │   └── ProductRepository.java
        │   │   └── service/
        │   │       ├── ProductService.java
        │   │       └── impl/ProductServiceImpl.java # Áp dụng @Cacheable, @CacheEvict
        │   └── resources/
        │       └── application.properties        # Cấu hình Postgres & Redis
        └── test/
            └── java/com/example/productservice/
                └── ProductServiceCacheTest.java  # Unit/Integration test kiểm chứng Cache-Aside & Evict
```

---

## 3. Hướng Dẫn Khởi Chạy

### Cách 1: Khởi động Redis & PostgreSQL qua Docker Compose (Khuyên dùng)
```bash
docker compose up -d
```
Lệnh trên sẽ khởi chạy:
- Redis server tại cổng `6379`
- PostgreSQL tại cổng `5432` (database: `product_db`, user: `postgres`, password: `17111998`)

### Cách 2: Chạy trực tiếp Product Service
```bash
# Windows
.\gradlew.bat :product-service:bootRun

# Linux / Mac
./gradlew :product-service:bootRun
```

---

## 4. Kiểm Thử API & Kiểm Chứng Kết Quả

Khi ứng dụng chạy, `DataInitializer` đã tạo sẵn 3 sản phẩm mẫu (ID: 1, 2, 3).

### Bước 1: Lần đầu truy cập (Cache Miss - Đọc Database)
```bash
curl -i http://localhost:8081/api/v1/products/1
```
**Kết quả**:
- Header: `X-Response-Time-Ms: ~200`
- Header: `X-Data-Source: DATABASE (Cache Miss)`
- Console log: `>>> [DATABASE QUERY] Cache Miss! Truy vấn SQL Database cho Product ID: 1`

### Bước 2: Các lần truy cập tiếp theo (Cache Hit - Đọc Redis)
```bash
curl -i http://localhost:8081/api/v1/products/1
```
**Kết quả**:
- Header: `X-Response-Time-Ms: < 10`
- Header: `X-Data-Source: REDIS_CACHE (Cache Hit)`
- Console log: Không xuất hiện log truy vấn Database, dữ liệu được trả về ngay lập tức từ Redis!

### Bước 3: Cập nhật sản phẩm & Xóa Cache (@CacheEvict)
```bash
curl -i -X PUT http://localhost:8081/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 16 Pro Max 512GB (Updated)",
    "description": "Bản nâng cấp dung lượng 512GB",
    "imageUrl": "https://cdn.example.com/images/iphone-16-pro-max-512.png",
    "price": 38990000,
    "stockQuantity": 30
  }'
```
- Console log: `>>> [CACHE EVICT] Bắt đầu cập nhật sản phẩm ID: 1 và xóa cache tương ứng`
- Cache `products::1` trong Redis bị xóa.

### Bước 4: Truy cập lại sau khi cập nhật
```bash
curl -i http://localhost:8081/api/v1/products/1
```
- Lần này lại đọc từ Database (~200ms) để lấy dữ liệu mới nhất, sau đó nạp lại vào Redis Cache với thông tin đã cập nhật.

---

## 5. Chạy Automated Tests
```bash
.\gradlew.bat :product-service:test
```
Bộ test `ProductServiceCacheTest` tự động verify:
1. `testCacheAsidePattern_GetProductById`: Cache miss gọi DB, Cache hit không gọi DB.
2. `testCacheEvict_OnUpdateProduct`: `@CacheEvict` dọn dẹp cache sau update và truy vấn DB ở lần gọi tiếp.

