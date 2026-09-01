# HinchMart Seller Backend APIs — Complete Specification (Revised)

**Date:** 2026-09-01  
**Status:** Ready for Implementation  
**Target Audience:** Backend Engineering Team

---

## Table of Contents
1. [API Groups Overview](#api-groups-overview)
2. [Global Response Format](#global-response-format)
3. [Global Error Handling](#global-error-handling)
4. [Category → Subcategory → Brand Hierarchy](#1-category--subcategory--brand-hierarchy)
5. [Seller-Isolated Product Management](#2-seller-isolated-product-management)
6. [Inventory & Warehouse Operations](#3-inventory--warehouse-operations)
7. [Bulk Price Adjustments](#4-bulk-price-adjustments)
8. [Document Vault](#5-document-vault)
9. [Buyer Enquiries & Quotations](#6-buyer-enquiries--quotations)

---

## API Groups Overview

| Group | Count | Purpose |
|-------|-------|---------|
| Category Hierarchy | 3 | Master data cascading selection |
| Product Management | 7 | Seller-owned product CRUD & filtering |
| Warehouse Operations | 3 | Logistics yard management & stock adjustments |
| Pricing | 1 | Bulk price updates across categories/brands |
| Documents | 2 | Compliance document uploads & management |
| Enquiries & Quotations | 3 | B2B RFQ handling & custom quotes |
| **TOTAL** | **19 APIs** | |

---

## Global Response Format

### Success Response (Standard)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation completed successfully",
  "data": {},
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

### Success Response (With Pagination)
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Records retrieved successfully",
  "data": [
    { "id": 1, "name": "..." }
  ],
  "pagination": {
    "total": 25,
    "page": 1,
    "limit": 12,
    "totalPages": 3
  },
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

### Error Response (Standard)
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "price",
      "message": "Price must be greater than 0"
    }
  ],
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

## Global Error Handling

| Status Code | Scenario | Example |
|-----------|----------|---------|
| **400** | Validation error, malformed request | Missing required field, invalid data type |
| **401** | Missing or invalid authentication token | Bearer token expired or missing |
| **403** | Authenticated but lacks permission | Seller trying to update product owned by another seller |
| **404** | Resource not found | Product ID doesn't exist |
| **409** | Conflict (duplicate SKU, etc.) | SKU already exists for this seller |
| **422** | Unprocessable entity (business logic violation) | Price > MRP, invalid bulk pricing tiers |
| **500** | Server error | Unexpected exception |

---

## 1. Category → Subcategory → Brand Hierarchy

Used for **Add Product** and **Product Filtering** workflows with strict dependent selection.

### 1.1 `GET /api/categories`

**Purpose:** Fetch all active master product categories  
**Auth:** Public / Optional Bearer Token  
**Query Parameters:** None  

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Categories retrieved successfully",
  "data": [
    {
      "id": 1,
      "categoryId": 1,
      "name": "Civil & Structural",
      "slug": "civil-structural",
      "description": "Building materials and structural components"
    },
    {
      "id": 2,
      "categoryId": 2,
      "name": "Electrical & Power",
      "slug": "electrical-power",
      "description": "Electrical and power distribution equipment"
    }
  ],
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

**Response `404 Not Found`:**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "No categories found",
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 1.2 `GET /api/subcategories`

**Purpose:** Fetch subcategories for a specific category  
**Auth:** Public / Optional Bearer Token  
**Query Parameters:**
- `categoryId` (required, integer)

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Subcategories retrieved successfully",
  "data": [
    {
      "id": 1,
      "subcategoryId": 1,
      "categoryId": 1,
      "name": "TMT Steel & Rebars",
      "slug": "tmt-steel-rebars"
    },
    {
      "id": 2,
      "subcategoryId": 2,
      "categoryId": 1,
      "name": "Cement & Concrete",
      "slug": "cement-concrete"
    }
  ],
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "categoryId",
      "message": "categoryId is required and must be a positive integer"
    }
  ],
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 1.3 `GET /api/brands`

**Purpose:** Fetch brands tagged for a specific subcategory  
**Auth:** Public / Optional Bearer Token  
**Query Parameters:**
- `subcategoryId` (required, integer)

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Brands retrieved successfully",
  "data": [
    {
      "id": 1,
      "brandId": 1,
      "subcategoryId": 1,
      "name": "Tata Tiscon",
      "slug": "tata-tiscon"
    },
    {
      "id": 2,
      "brandId": 2,
      "subcategoryId": 1,
      "name": "JSW Neosteel",
      "slug": "jsw-neosteel"
    }
  ],
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

## 2. Seller-Isolated Product Management

⚠️ **CRITICAL DATA ISOLATION:**
- Backend derives `sellerId` from authenticated JWT (`req.user.sellerId`)
- `GET /api/seller/products` must **never** return Admin-created or other sellers' products
- If seller has 0 products, return empty array `[]`
- Cross-seller product access must return `403 Forbidden`

### 2.1 `GET /api/seller/products`

**Purpose:** List all products created by authenticated seller  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Derived From:** JWT claims (`req.user.sellerId`)  

**Query Parameters (Optional):**
- `search` — string (searches title, SKU, description)
- `categoryId` — integer
- `subcategoryId` — integer
- `brandId` — integer
- `status` — `PENDING` | `APPROVED` | `REJECTED`
- `stockStatus` — `In Stock` | `Low Stock` | `Out of Stock`
- `sortBy` — `newest` | `oldest` | `price-asc` | `price-desc` | `stock-asc` | `stock-desc`
- `page` — integer (default: 1, min: 1)
- `limit` — integer (default: 12, max: 100)

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Products retrieved successfully",
  "data": [
    {
      "id": "sp_101",
      "sellerId": "seller_1001",
      "title": "Tata Tiscon 550D TMT Steel Rebars 16mm",
      "sku": "SKU-TATA-550D-16MM",
      "description": "High-ductility Fe 550D grade seismic resistant steel rebars.",
      "brandId": 1,
      "brandName": "Tata Tiscon",
      "categoryId": 1,
      "categoryName": "Civil & Structural",
      "subcategoryId": 1,
      "subcategoryName": "TMT Steel & Rebars",
      "price": 64500,
      "sellingPrice": 64500,
      "mrp": 69000,
      "unit": "Ton",
      "moq": 5,
      "stockQty": 85,
      "is24HourDelivery": true,
      "status": "APPROVED",
      "images": [
        "https://storage.hinchmart.com/products/sp_101_img_1.jpg"
      ],
      "bulkPricingTiers": [
        { "minQty": 5, "maxQty": 19, "price": 64500, "discount": 0 },
        { "minQty": 20, "maxQty": 100, "price": 62500, "discount": 3.1 }
      ],
      "specifications": {
        "Grade": "Fe 550D",
        "Standard": "IS 1786:2008",
        "Diameter": "16 mm"
      },
      "createdAt": "2026-08-25T10:00:00.000Z",
      "updatedAt": "2026-09-01T12:00:00.000Z"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 12,
    "totalPages": 1
  },
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

**Response `401 Unauthorized`:**
```json
{
  "success": false,
  "statusCode": 401,
  "message": "Authentication required. Please provide a valid JWT token.",
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

**Response `403 Forbidden`:**
```json
{
  "success": false,
  "statusCode": 403,
  "message": "Access denied. Invalid or expired token.",
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 2.2 `GET /api/seller/products/{id}`

**Purpose:** Fetch single product with ownership verification  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Path Parameters:**
- `id` — string (product ID, e.g., `sp_101`)

**Response `200 OK`:** Same schema as single product object from 2.1

**Response `403 Forbidden`:**
```json
{
  "success": false,
  "statusCode": 403,
  "message": "Access denied. This product is not owned by you.",
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

**Response `404 Not Found`:**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Product not found.",
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 2.3 `POST /api/seller/products`

**Purpose:** Create new seller product (initially `PENDING` admin review)  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Content-Type:** `multipart/form-data` or `application/json`

**Request Body:**
```json
{
  "title": "Ultratech Super Cement 50kg Bag",
  "sku": "SKU-ULTRA-PPC-50KG",
  "description": "Engineered PPC cement designed for superior compressive strength.",
  "categoryId": 1,
  "subcategoryId": 2,
  "brandId": 2,
  "price": 395,
  "sellingPrice": 395,
  "mrp": 440,
  "unit": "Bags",
  "moq": 50,
  "stockQty": 400,
  "is24HourDelivery": true,
  "images": [
    "https://storage.hinchmart.com/products/ultra_cement_1.jpg"
  ],
  "bulkPricingTiers": [
    { "minQty": 50, "maxQty": 199, "price": 395, "discount": 0 },
    { "minQty": 200, "maxQty": 1000, "price": 380, "discount": 3.8 }
  ],
  "specifications": {
    "Grade": "PPC Grade",
    "Packaging": "50 kg HDPE Bag"
  }
}
```

**Validation Rules:**
- `title`: Required, min 10 chars, max 255 chars
- `sku`: Required, unique per seller, max 50 chars, alphanumeric + hyphens
- `description`: Required, min 20 chars, max 2000 chars
- `categoryId`, `subcategoryId`, `brandId`: Required, must exist in master data
- `price`: Required, > 0, must be ≤ MRP
- `sellingPrice`: Required, > 0, must be ≤ MRP
- `mrp`: Required, > 0
- `moq`: Required, ≥ 1
- `stockQty`: Required, ≥ 0
- `unit`: Required, max 50 chars
- `bulkPricingTiers`: Optional, must be sorted by `minQty` ascending, prices must be > 0

**Response `201 Created`:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Product submitted successfully for admin review.",
  "data": {
    "id": "sp_102",
    "sellerId": "seller_1001",
    "title": "Ultratech Super Cement 50kg Bag",
    "sku": "SKU-ULTRA-PPC-50KG",
    "status": "PENDING",
    "createdAt": "2026-09-01T17:15:00.000Z"
  },
  "timestamp": "2026-09-01T17:15:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "sku",
      "message": "SKU 'SKU-ULTRA-PPC-50KG' already exists for your account"
    },
    {
      "field": "price",
      "message": "Price (395) cannot be greater than MRP (440)"
    },
    {
      "field": "bulkPricingTiers",
      "message": "Bulk pricing tiers must be sorted by minQty in ascending order"
    }
  ],
  "timestamp": "2026-09-01T17:15:00.000Z"
}
```

**Response `409 Conflict`:**
```json
{
  "success": false,
  "statusCode": 409,
  "message": "Duplicate SKU. This SKU already exists in your inventory.",
  "timestamp": "2026-09-01T17:15:00.000Z"
}
```

---

### 2.4 `PUT /api/seller/products/{id}`

**Purpose:** Update existing product information  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Path Parameters:**
- `id` — string (product ID)

**Request Body:** Any updatable fields
```json
{
  "title": "Updated Product Title",
  "description": "Updated description",
  "sellingPrice": 410,
  "mrp": 450,
  "moq": 40,
  "stockQty": 350,
  "bulkPricingTiers": [
    { "minQty": 40, "maxQty": 199, "price": 410, "discount": 0 }
  ],
  "specifications": {
    "Grade": "Updated Grade"
  }
}
```

**Non-Updatable Fields:**
- `id`, `sellerId`, `sku`, `categoryId`, `subcategoryId`, `brandId`, `status` (admin-only), `createdAt`

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Product updated successfully",
  "data": {
    "id": "sp_102",
    "title": "Updated Product Title",
    "updatedAt": "2026-09-01T18:00:00.000Z"
  },
  "timestamp": "2026-09-01T18:00:00.000Z"
}
```

**Response `403 Forbidden`:**
```json
{
  "success": false,
  "statusCode": 403,
  "message": "Cannot update product. Status is APPROVED; contact admin for changes.",
  "timestamp": "2026-09-01T18:00:00.000Z"
}
```

---

### 2.5 `PATCH /api/seller/products/{id}/stock`

**Purpose:** Quick single-click stock quantity update  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Path Parameters:**
- `id` — string (product ID)

**Request Body:**
```json
{
  "stockQty": 150
}
```

**Validation:**
- `stockQty`: Required, must be ≥ 0

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Stock updated successfully",
  "data": {
    "id": "sp_102",
    "stockQty": 150,
    "updatedAt": "2026-09-01T18:05:00.000Z"
  },
  "timestamp": "2026-09-01T18:05:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "stockQty",
      "message": "Stock quantity must be >= 0"
    }
  ],
  "timestamp": "2026-09-01T18:05:00.000Z"
}
```

---

### 2.6 `PATCH /api/seller/products/{id}/pricing`

**Purpose:** Quick price and bulk tier update  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Path Parameters:**
- `id` — string (product ID)

**Request Body:**
```json
{
  "sellingPrice": 390,
  "mrp": 440,
  "bulkPricingTiers": [
    { "minQty": 50, "maxQty": 199, "price": 390, "discount": 0 },
    { "minQty": 200, "maxQty": 1000, "price": 375, "discount": 3.8 }
  ]
}
```

**Validation:**
- `sellingPrice`: Must be > 0, ≤ MRP
- `mrp`: Must be > 0
- `bulkPricingTiers`: Must be sorted by `minQty` ascending

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Product pricing updated successfully",
  "data": {
    "id": "sp_102",
    "sellingPrice": 390,
    "mrp": 440,
    "updatedAt": "2026-09-01T18:10:00.000Z"
  },
  "timestamp": "2026-09-01T18:10:00.000Z"
}
```

---

### 2.7 `DELETE /api/seller/products/{id}`

**Purpose:** Delete or archive seller product  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Path Parameters:**
- `id` — string (product ID)

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Product removed from inventory",
  "data": {
    "id": "sp_102",
    "deletedAt": "2026-09-01T18:15:00.000Z"
  },
  "timestamp": "2026-09-01T18:15:00.000Z"
}
```

**Response `404 Not Found`:**
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Product not found",
  "timestamp": "2026-09-01T18:15:00.000Z"
}
```

---

## 3. Inventory & Warehouse Operations

### 3.1 `GET /api/seller/warehouses`

**Purpose:** List seller's registered logistics warehouses  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Query Parameters:** None

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Warehouses retrieved successfully",
  "data": [
    {
      "id": "wh_1",
      "name": "Bhiwandi Central Logistics Yard",
      "isDefault": true,
      "contactPerson": "Suresh Patil",
      "phone": "+91 98201 11223",
      "city": "Bhiwandi",
      "state": "Maharashtra",
      "pincode": "421302",
      "address": "Plot C-14, Mankoli Logistics Hub, Bhiwandi",
      "capacityTons": 15000,
      "currentLoadTons": 8500,
      "status": "Active",
      "createdAt": "2026-08-15T10:00:00.000Z"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 50,
    "totalPages": 1
  },
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 3.2 `POST /api/seller/warehouses`

**Purpose:** Add new warehouse/depot  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Request Body:**
```json
{
  "name": "Chakan Industrial Depot",
  "contactPerson": "Mahesh Deshmukh",
  "phone": "+91 98202 22334",
  "city": "Pune",
  "state": "Maharashtra",
  "pincode": "410501",
  "address": "Phase 2 MIDC, Chakan Industrial Area, Pune",
  "capacityTons": 8000,
  "isDefault": false
}
```

**Validation:**
- `name`: Required, min 5 chars, max 100 chars
- `contactPerson`: Required, min 3 chars, max 100 chars
- `phone`: Required, valid Indian phone format
- `city`: Required, max 50 chars
- `state`: Required, valid Indian state
- `pincode`: Required, 6-digit Indian pincode
- `address`: Required, min 10 chars, max 500 chars
- `capacityTons`: Required, > 0

**Response `201 Created`:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Warehouse added successfully",
  "data": {
    "id": "wh_2",
    "name": "Chakan Industrial Depot",
    "status": "Active",
    "createdAt": "2026-09-01T19:00:00.000Z"
  },
  "timestamp": "2026-09-01T19:00:00.000Z"
}
```

---

### 3.3 `POST /api/seller/inventory/adjust`

**Purpose:** Log stock adjustments (inbound, outbound, audit corrections)  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Request Body:**
```json
{
  "productId": "sp_101",
  "warehouseId": "wh_1",
  "adjustmentType": "add",
  "quantity": 25,
  "reason": "New mill batch received from Tata Steel plant"
}
```

**Validation:**
- `productId`: Required, must exist and be owned by seller
- `warehouseId`: Required, must exist and be owned by seller
- `adjustmentType`: Required, one of: `add` | `deduct` | `audit_correction`
- `quantity`: Required, integer > 0
- `reason`: Required, max 500 chars

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Stock adjusted successfully",
  "data": {
    "productId": "sp_101",
    "warehouseId": "wh_1",
    "adjustmentType": "add",
    "quantity": 25,
    "previousStock": 85,
    "newStock": 110,
    "adjustedAt": "2026-09-01T19:05:00.000Z"
  },
  "timestamp": "2026-09-01T19:05:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "adjustmentType",
      "message": "adjustmentType must be one of: add, deduct, audit_correction"
    }
  ],
  "timestamp": "2026-09-01T19:05:00.000Z"
}
```

---

## 4. Bulk Price Adjustments

### 4.1 `POST /api/seller/pricing/bulk-adjust`

**Purpose:** Apply percentage/fixed price adjustments across Category or Brand  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Request Body:**
```json
{
  "categoryId": 1,
  "brandId": null,
  "adjustmentType": "percentage_increase",
  "value": 3.5,
  "applyTo": "selling_price"
}
```

**Parameters:**
- `categoryId`: integer (null if using `brandId`)
- `brandId`: integer (null if using `categoryId`)
  - Must specify **one of** `categoryId` or `brandId`, not both
- `adjustmentType`: Required, one of:
  - `percentage_increase` | `percentage_decrease` | `fixed_increase` | `fixed_decrease`
- `value`: Required, number > 0
- `applyTo`: Required, one of:
  - `selling_price` | `mrp` | `both`

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Bulk price adjustment applied to 4 products",
  "data": {
    "modifiedCount": 4,
    "adjustmentType": "percentage_increase",
    "value": 3.5,
    "applyTo": "selling_price",
    "summary": {
      "categoryId": 1,
      "categoryName": "Civil & Structural",
      "oldAvgPrice": 64500,
      "newAvgPrice": 66735,
      "totalValueChange": "+9270"
    },
    "modifiedProducts": [
      {
        "productId": "sp_101",
        "title": "Tata Tiscon 550D TMT Steel Rebars 16mm",
        "oldPrice": 64500,
        "newPrice": 66735
      }
    ]
  },
  "timestamp": "2026-09-01T19:15:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "categoryId/brandId",
      "message": "Must specify either categoryId or brandId, not both"
    }
  ],
  "timestamp": "2026-09-01T19:15:00.000Z"
}
```

---

## 5. Document Vault

### 5.1 `GET /api/seller/documents`

**Purpose:** List uploaded seller compliance documents  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Documents retrieved successfully",
  "data": [
    {
      "id": "doc_1",
      "documentType": "GSTIN",
      "name": "GST Registration Certificate",
      "fileName": "GST_Registration_2026.pdf",
      "fileUrl": "https://storage.hinchmart.com/docs/gst_1001.pdf",
      "fileSize": 245120,
      "status": "APPROVED",
      "uploadedAt": "2026-08-20T08:30:00.000Z"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 50,
    "totalPages": 1
  },
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 5.2 `POST /api/seller/documents`

**Purpose:** Upload compliance document to vault  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`  
**Content-Type:** `multipart/form-data`

**Form Fields:**
- `documentType` (required): `GSTIN` | `PAN` | `INCORPORATION` | `MSME` | `TRADE_LICENSE`
- `file` (required): Binary file

**File Constraints:**
- **Allowed MIME Types:** `application/pdf`, `image/jpeg`, `image/png`
- **Max File Size:** 5 MB
- **Naming:** Alphanumeric, hyphens, underscores only
- **Security:** Files scanned for viruses before storage

**Response `201 Created`:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Document uploaded and queued for verification",
  "data": {
    "id": "doc_3",
    "documentType": "MSME",
    "fileName": "Udyam_MSME.pdf",
    "fileSize": 189456,
    "status": "PENDING",
    "uploadedAt": "2026-09-01T19:20:00.000Z"
  },
  "timestamp": "2026-09-01T19:20:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "file",
      "message": "File size exceeds 5 MB limit"
    },
    {
      "field": "file",
      "message": "Only PDF, JPEG, and PNG files are allowed"
    }
  ],
  "timestamp": "2026-09-01T19:20:00.000Z"
}
```

---

## 6. Buyer Enquiries & Custom Quotations

### 6.1 `GET /api/seller/enquiries`

**Purpose:** List RFQs and bulk project enquiries from buyers  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Query Parameters (Optional):**
- `status` — `NEW` | `QUOTED` | `ACCEPTED` | `REJECTED`
- `sortBy` — `newest` | `oldest`
- `page` — integer (default: 1)
- `limit` — integer (default: 12)

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Enquiries retrieved successfully",
  "data": [
    {
      "id": "enq_2026_01",
      "buyerName": "L&T Construction Infra Project",
      "projectName": "Metro Rail Phase 2 Pier Construction",
      "buyerCity": "Bengaluru",
      "buyerState": "Karnataka",
      "requestedItems": [
        {
          "productName": "Tata Tiscon 550D 16mm",
          "quantity": 100,
          "unit": "Ton"
        }
      ],
      "status": "NEW",
      "createdAt": "2026-09-01T14:30:00.000Z"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 12,
    "totalPages": 1
  },
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

### 6.2 `POST /api/seller/quotations`

**Purpose:** Create and dispatch custom B2B quotation to buyer  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Request Body:**
```json
{
  "enquiryId": "enq_2026_01",
  "buyerName": "L&T Construction Infra Project",
  "buyerEmail": "procurement@intec.lnt.com",
  "validUntil": "2026-09-15",
  "items": [
    {
      "productId": "sp_101",
      "name": "Tata Tiscon 550D TMT Steel Rebars 16mm",
      "quantity": 100,
      "unit": "Ton",
      "quotedRate": 61500,
      "gstRate": 18
    }
  ],
  "freightCharges": 25000,
  "paymentTerms": "50% Advance, 50% on Delivery",
  "deliveryTimeline": "3 Business Days",
  "notes": "Special rate for project order. Valid for 14 days."
}
```

**Validation:**
- `enquiryId`: Required if responding to existing enquiry
- `buyerEmail`: Required, valid email format
- `validUntil`: Required, date >= today, ISO 8601 format
- `items`: Required, min 1 item
- `items[].productId`: Must be owned by seller
- `items[].quantity`: > 0
- `items[].quotedRate`: > 0
- `items[].gstRate`: 0-28 percent
- `freightCharges`: >= 0
- `paymentTerms`: Required, max 200 chars
- `deliveryTimeline`: Required, max 100 chars

**Calculation:**
```
itemTotal = quantity * quotedRate
itemGST = itemTotal * (gstRate / 100)
itemAmount = itemTotal + itemGST
totalAmount = Sum(itemAmount for all items) + freightCharges
```

**Response `201 Created`:**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Quotation created and dispatched to buyer successfully",
  "data": {
    "id": "quot_2026_884",
    "quotationNumber": "QUOT-HM-2026-884",
    "enquiryId": "enq_2026_01",
    "buyerEmail": "procurement@intec.lnt.com",
    "status": "SENT",
    "itemCount": 1,
    "totalAmount": 7282000,
    "calculation": {
      "itemsSubtotal": 6150000,
      "gstOnItems": 1107000,
      "freightCharges": 25000,
      "grandTotal": 7282000
    },
    "validUntil": "2026-09-15",
    "sentAt": "2026-09-01T19:30:00.000Z",
    "emailSentAt": "2026-09-01T19:30:15.000Z"
  },
  "timestamp": "2026-09-01T19:30:00.000Z"
}
```

**Response `400 Bad Request`:**
```json
{
  "success": false,
  "statusCode": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "validUntil",
      "message": "Quotation validity date must be in the future"
    },
    {
      "field": "items[0].productId",
      "message": "Product sp_101 not found or not owned by you"
    }
  ],
  "timestamp": "2026-09-01T19:30:00.000Z"
}
```

---

### 6.3 `GET /api/seller/quotations`

**Purpose:** List all quotations dispatched by this seller  
**Auth:** Required — `Authorization: Bearer <JWT_TOKEN>`

**Query Parameters (Optional):**
- `status` — `SENT` | `VIEWED` | `ACCEPTED` | `EXPIRED` | `CONVERTED_TO_ORDER`
- `sortBy` — `newest` | `oldest`
- `page` — integer (default: 1)
- `limit` — integer (default: 12)

**Response `200 OK`:**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Quotations retrieved successfully",
  "data": [
    {
      "id": "quot_2026_884",
      "quotationNumber": "QUOT-HM-2026-884",
      "buyerName": "L&T Construction Infra Project",
      "buyerEmail": "procurement@intec.lnt.com",
      "status": "SENT",
      "totalAmount": 7282000,
      "itemCount": 1,
      "validUntil": "2026-09-15",
      "sentAt": "2026-09-01T19:30:00.000Z",
      "viewedAt": null,
      "acceptedAt": null
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 12,
    "totalPages": 1
  },
  "timestamp": "2026-09-01T15:30:00.000Z"
}
```

---

## Summary: Implementation Checklist

### Entities Required
- [ ] Category
- [ ] Subcategory
- [ ] Brand
- [ ] SellerProduct
- [ ] BulkPricingTier
- [ ] Warehouse
- [ ] InventoryAdjustment
- [ ] SellerDocument
- [ ] Enquiry
- [ ] Quotation
- [ ] QuotationItem

### Controllers Required
- [ ] CategoryController
- [ ] SubcategoryController
- [ ] BrandController
- [ ] SellerProductController
- [ ] WarehouseController
- [ ] InventoryController
- [ ] PricingController
- [ ] DocumentController
- [ ] EnquiryController
- [ ] QuotationController

### Services Required
- [ ] CategoryService
- [ ] SellerProductService
- [ ] WarehouseService
- [ ] InventoryService
- [ ] PricingService
- [ ] DocumentService
- [ ] EnquiryService
- [ ] QuotationService

### Repositories Required
- [ ] CategoryRepository
- [ ] SubcategoryRepository
- [ ] BrandRepository
- [ ] SellerProductRepository
- [ ] WarehouseRepository
- [ ] InventoryAdjustmentRepository
- [ ] SellerDocumentRepository
- [ ] EnquiryRepository
- [ ] QuotationRepository

---

**Generated:** 2026-09-01  
**Version:** 1.0 (Revised & Corrected)
