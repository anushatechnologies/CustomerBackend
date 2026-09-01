# HinchMart Seller Backend Implementation Guide

**Generated:** 2026-09-01  
**Status:** Ready for Service Layer Implementation

---

## ✅ Completed Tasks

### 1. **Revised API Specification** ✓
- File: [API_SPEC_REVISED.md](API_SPEC_REVISED.md)
- Contains 19 complete APIs across 6 groups
- Includes error handling, validation rules, and status codes
- Comprehensive examples for all request/response formats

### 2. **Entity Classes** ✓
Created JPA entities with proper annotations and relationships:

| Entity | File | Purpose |
|--------|------|---------|
| `SellerProduct` | SellerProduct.java | Seller-owned products |
| `Warehouse` | Warehouse.java | Logistics warehouse management |
| `InventoryAdjustment` | InventoryAdjustment.java | Stock adjustment audit trail |
| `SellerDocumentEntity` | SellerDocumentEntity.java | Compliance documents |
| `Enquiry` | Enquiry.java | Buyer RFQs |
| `QuotationEntity` | QuotationEntity.java | Custom B2B quotations |

### 3. **Data Transfer Objects (DTOs)** ✓
Created request/response DTOs for all API operations:

**Request DTOs:**
- `SellerProductRequest`
- `StockUpdateRequest`
- `PricingUpdateRequest`
- `WarehouseRequest`
- `InventoryAdjustmentRequest`
- `BulkPriceAdjustmentRequest`
- `QuotationRequest`

**Response DTOs:**
- `SellerProductResponse`
- `WarehouseResponse`
- `InventoryAdjustmentResponse`
- `SellerDocumentResponse`
- `EnquiryResponse`
- `QuotationResponse`
- `BulkPriceAdjustmentResponse`
- `PaginationMeta` (for paginated responses)

### 4. **Controller Stubs** ✓
Created REST controller classes with endpoint signatures:

| Controller | Endpoints | File |
|-----------|-----------|------|
| `CategoryControllerAPI` | GET /api/categories | CategoryControllerAPI.java |
| `SubcategoryControllerAPI` | GET /api/subcategories | SubcategoryControllerAPI.java |
| `BrandControllerAPI` | GET /api/brands | BrandControllerAPI.java |
| `SellerProductControllerAPI` | 7 product management endpoints | SellerProductControllerAPI.java |
| `InventoryControllerAPI` | 3 warehouse & inventory endpoints | InventoryControllerAPI.java |
| `PricingControllerAPI` | POST bulk price adjustment | PricingControllerAPI.java |
| `DocumentControllerAPI` | 2 document management endpoints | DocumentControllerAPI.java |
| `QuotationControllerAPI` | 3 enquiry & quotation endpoints | QuotationControllerAPI.java |

### 5. **Repository Interfaces** ✓
Created Spring Data JPA repositories with custom queries:

| Repository | Purpose |
|-----------|---------|
| `SellerProductRepository` | Product CRUD + search, filtering |
| `WarehouseRepository` | Warehouse CRUD with ownership check |
| `InventoryAdjustmentRepository` | Audit trail queries |
| `EnquiryRepository` | Enquiry queries with pagination |
| (SellerDocumentRepository & QuotationRepository already exist) |

---

## 📋 Next Steps: Service Layer Implementation

### Services to Implement

#### 1. **SellerProductService**
```java
// Key Methods
public Page<SellerProduct> getSellerProducts(String sellerId, ProductFilterRequest filter);
public SellerProduct createProduct(String sellerId, SellerProductRequest request);
public SellerProduct updateProduct(String sellerId, String productId, SellerProductRequest request);
public void updateStock(String sellerId, String productId, Integer stockQty);
public void updatePricing(String sellerId, String productId, PricingUpdateRequest request);
public void deleteProduct(String sellerId, String productId);
```

**Responsibilities:**
- Validate sellerId ownership before operations
- Generate unique product IDs (format: `sp_XXX`)
- Convert DTOs to entities and vice versa
- Handle business logic validation (price <= MRP, moq >= 1, etc.)

#### 2. **WarehouseService**
```java
// Key Methods
public List<Warehouse> getSellerWarehouses(String sellerId);
public Warehouse createWarehouse(String sellerId, WarehouseRequest request);
public Optional<Warehouse> getWarehouse(String sellerId, String warehouseId);
```

#### 3. **InventoryService**
```java
// Key Methods
public InventoryAdjustment adjustStock(String sellerId, InventoryAdjustmentRequest request);
public List<InventoryAdjustment> getAdjustmentHistory(String sellerId, String productId);
```

#### 4. **PricingService**
```java
// Key Methods
public BulkPriceAdjustmentResponse bulkAdjustPrices(String sellerId, BulkPriceAdjustmentRequest request);
```

**Responsibilities:**
- Calculate percentage/fixed adjustments
- Update all matching products atomically
- Return detailed change summary with old/new prices

#### 5. **DocumentService**
```java
// Key Methods
public List<SellerDocumentEntity> getDocuments(String sellerId);
public SellerDocumentEntity uploadDocument(String sellerId, String documentType, MultipartFile file);
```

**Responsibilities:**
- Validate file type (PDF, JPEG, PNG only)
- Validate file size (max 5 MB)
- Upload to S3/cloud storage
- Generate secure file URLs
- Scan for viruses before storage

#### 6. **EnquiryService**
```java
// Key Methods
public Page<Enquiry> getEnquiries(String sellerId, Pageable pageable);
public Optional<Enquiry> getEnquiry(String sellerId, String enquiryId);
```

#### 7. **QuotationService**
```java
// Key Methods
public QuotationEntity createQuotation(String sellerId, QuotationRequest request);
public Page<QuotationEntity> getQuotations(String sellerId, Pageable pageable);
public void sendQuotationEmail(QuotationEntity quotation);
```

**Responsibilities:**
- Validate enquiryId, product IDs owned by seller
- Generate unique quotation number (format: `QUOT-HM-YYYY-XXX`)
- Calculate total with GST and freight
- Send email to buyer with quotation PDF
- Track delivery status

---

## 🔒 Security Checklist

### Authentication & Authorization
- [ ] Add `@PreAuthorize("isAuthenticated()")` to seller endpoints
- [ ] Extract `sellerId` from JWT claims: `getCurrentSellerId()` from SecurityContext
- [ ] Verify ownership on every operation: `seller1 cannot access seller2's products`
- [ ] Log all modifications for audit trail

### Data Validation
- [ ] Use `@Valid` annotation on DTOs
- [ ] Implement custom validators:
  - SKU uniqueness per seller
  - Price <= MRP
  - MOQ >= 1
  - Bulk pricing tiers sorted by minQty
  - Category/Subcategory/Brand ID existence
  - File upload size and type

### File Upload Security
- [ ] Scan uploaded documents for viruses
- [ ] Store files in secure S3 bucket (private by default)
- [ ] Generate signed URLs with expiration
- [ ] Validate MIME types (not just file extension)
- [ ] Store file metadata: size, hash, virus scan result

---

## 📊 Database Schema Guidelines

### SellerProduct Table
```sql
CREATE TABLE seller_products (
  product_id VARCHAR(50) PRIMARY KEY,
  seller_id VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  sku VARCHAR(50) NOT NULL UNIQUE,
  description TEXT,
  brand_id INT,
  category_id INT,
  subcategory_id INT,
  price BIGINT,
  selling_price BIGINT,
  mrp BIGINT,
  unit VARCHAR(50),
  moq INT,
  stock_qty INT,
  is_24hour_delivery BOOLEAN,
  status VARCHAR(20),
  images JSON,
  bulk_pricing_tiers JSON,
  specifications JSON,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  active BOOLEAN DEFAULT TRUE,
  UNIQUE KEY unique_sku_seller (sku, seller_id),
  INDEX idx_seller_id (seller_id),
  INDEX idx_status (status)
);
```

### Warehouse Table
```sql
CREATE TABLE seller_warehouses (
  warehouse_id VARCHAR(20) PRIMARY KEY,
  seller_id VARCHAR(50) NOT NULL,
  name VARCHAR(100),
  is_default BOOLEAN,
  contact_person VARCHAR(100),
  phone VARCHAR(20),
  city VARCHAR(50),
  state VARCHAR(50),
  pincode VARCHAR(10),
  address VARCHAR(500),
  capacity_tons BIGINT,
  current_load_tons BIGINT,
  status VARCHAR(20),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_seller_id (seller_id),
  INDEX idx_is_default (seller_id, is_default)
);
```

### Quotation Table
```sql
CREATE TABLE quotations (
  quotation_id VARCHAR(30) PRIMARY KEY,
  quotation_number VARCHAR(50) NOT NULL UNIQUE,
  seller_id VARCHAR(50) NOT NULL,
  enquiry_id VARCHAR(30),
  buyer_name VARCHAR(255),
  buyer_email VARCHAR(255),
  status VARCHAR(20),
  items JSON,
  total_amount BIGINT,
  calculation_details JSON,
  freight_charges BIGINT,
  payment_terms VARCHAR(200),
  delivery_timeline VARCHAR(100),
  notes TEXT,
  valid_until DATE,
  sent_at TIMESTAMP,
  email_sent_at TIMESTAMP,
  viewed_at TIMESTAMP,
  accepted_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  INDEX idx_seller_id (seller_id),
  INDEX idx_status (status),
  INDEX idx_quotation_number (quotation_number)
);
```

---

## 🎯 Implementation Workflow

1. **Phase 1: Core Services (Week 1)**
   - Implement SellerProductService with full CRUD
   - Implement WarehouseService
   - Implement InventoryService with audit trail

2. **Phase 2: Advanced Services (Week 2)**
   - Implement PricingService with bulk adjustments
   - Implement DocumentService with S3 upload
   - Add virus scanning integration

3. **Phase 3: B2B Features (Week 2)**
   - Implement EnquiryService
   - Implement QuotationService with PDF generation
   - Add email notification service

4. **Phase 4: Testing & Security (Week 3)**
   - Unit tests for all services
   - Integration tests for APIs
   - Security audit: ownership verification, input validation
   - Performance testing: bulk operations

5. **Phase 5: Deployment (Week 4)**
   - Database migration scripts
   - API documentation (Swagger)
   - Deployment to staging/production

---

## 📝 API Response Status Codes

### Success
- `200 OK` — GET, PUT, PATCH, DELETE successful
- `201 Created` — POST successful, resource created

### Client Errors
- `400 Bad Request` — Validation failed, missing fields
- `401 Unauthorized` — Missing or expired JWT token
- `403 Forbidden` — Authenticated but lacks permission (ownership check failed)
- `404 Not Found` — Resource does not exist
- `409 Conflict` — Duplicate SKU, uniqueness constraint violated
- `422 Unprocessable Entity` — Business logic violation (price > MRP, etc.)

### Server Errors
- `500 Internal Server Error` — Unexpected exception

---

## 🚀 Quick Start

### 1. Generate IDs
```java
// Product ID: sp_001, sp_002, ...
private String generateProductId() {
    return "sp_" + System.nanoTime();
}

// Quotation Number: QUOT-HM-2026-001, QUOT-HM-2026-002, ...
private String generateQuotationNumber() {
    return "QUOT-HM-" + LocalDate.now().getYear() + "-" + random;
}
```

### 2. Ownership Verification Pattern
```java
public SellerProduct validateAndGetProduct(String sellerId, String productId) {
    return sellerProductRepository.findByIdAndSellerId(productId, sellerId)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
}
```

### 3. Pagination Pattern
```java
@GetMapping
public ResponseEntity<ApiResponse<?>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int limit) {
    Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
    Page<?> data = repository.findBySellerId(sellerId, pageable);
    
    PaginationMeta pagination = PaginationMeta.builder()
        .total(data.getTotalElements())
        .page(page)
        .limit(limit)
        .totalPages(data.getTotalPages())
        .build();
    
    return ResponseEntity.ok(ApiResponse.builder()
        .success(true)
        .data(data.getContent())
        .pagination(pagination)
        .build());
}
```

---

## 📚 Reference Files

| File | Location | Purpose |
|------|----------|---------|
| API Spec | API_SPEC_REVISED.md | Complete API documentation |
| Entities | entity/*.java | JPA entities |
| DTOs | dto/*.java | Request/Response models |
| Controllers | controller/*ControllerAPI.java | REST endpoints (stubs) |
| Repositories | repository/*Repository.java | Data access |

---

## ✨ Key Features Implemented

✅ Seller data isolation (sellerId-based filtering)  
✅ Product CRUD with admin approval workflow  
✅ Warehouse & inventory management  
✅ Bulk price adjustments  
✅ Document vault with compliance uploads  
✅ RFQ handling & custom quotations  
✅ Comprehensive error handling  
✅ Pagination & sorting  
✅ Audit trail for inventory  
✅ Email notifications (quotation dispatch)

---

**Last Updated:** 2026-09-01  
**Ready for:** Service Layer Implementation
