# HinchMart APIs - Postman Testing Guide

**Base URL:** `http://localhost:9000`  
**Port:** `9000`  
**Total APIs:** 19

---

## Authentication

All authenticated endpoints require:
```
Authorization: Bearer <JWT_TOKEN>
```

> Replace `<JWT_TOKEN>` with a valid JWT token from your auth endpoint

---

## 1. Category Hierarchy APIs

### 1.1 GET /api/categories

**URL:** `http://localhost:9000/api/categories`  
**Method:** GET  
**Auth:** Not Required  
**Query Params:** None

**Response (200):**
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

### 1.2 GET /api/subcategories

**URL:** `http://localhost:9000/api/subcategories?categoryId=1`  
**Method:** GET  
**Auth:** Not Required  
**Query Params:**
- `categoryId` (required) = 1

**Response (200):**
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

### 1.3 GET /api/brands

**URL:** `http://localhost:9000/api/brands?subcategoryId=1`  
**Method:** GET  
**Auth:** Not Required  
**Query Params:**
- `subcategoryId` (required) = 1

**Response (200):**
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

## 2. Seller Product Management APIs

### 2.1 GET /api/seller/products

**URL:** `http://localhost:9000/api/seller/products?page=1&limit=12&status=APPROVED`  
**Method:** GET  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Query Params:**
- `search` (optional) = "Tata Tiscon"
- `categoryId` (optional) = 1
- `subcategoryId` (optional) = 1
- `brandId` (optional) = 1
- `status` (optional) = PENDING | APPROVED | REJECTED
- `stockStatus` (optional) = In Stock | Low Stock | Out of Stock
- `sortBy` (optional) = newest | oldest | price-asc | price-desc
- `page` (optional) = 1
- `limit` (optional) = 12

**Response (200):**
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
      "images": ["https://storage.hinchmart.com/products/sp_101_img_1.jpg"],
      "bulkPricingTiers": [
        {
          "minQty": 5,
          "maxQty": 19,
          "price": 64500,
          "discount": 0
        },
        {
          "minQty": 20,
          "maxQty": 100,
          "price": 62500,
          "discount": 3.1
        }
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

### 2.2 GET /api/seller/products/{id}

**URL:** `http://localhost:9000/api/seller/products/sp_101`  
**Method:** GET  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Path Params:**
- `id` = sp_101

**Response (200):** Same as 2.1 single product object

---

### 2.3 POST /api/seller/products

**URL:** `http://localhost:9000/api/seller/products`  
**Method:** POST  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (JSON):**
```json
{
  "title": "Ultratech Super Cement 50kg Bag",
  "sku": "SKU-ULTRA-PPC-50KG",
  "description": "Engineered PPC cement designed for superior compressive strength and durability in all weather conditions.",
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
    {
      "minQty": 50,
      "maxQty": 199,
      "price": 395,
      "discount": 0
    },
    {
      "minQty": 200,
      "maxQty": 1000,
      "price": 380,
      "discount": 3.8
    }
  ],
  "specifications": {
    "Grade": "PPC Grade",
    "Packaging": "50 kg HDPE Bag"
  }
}
```

**Response (201):**
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
    "createdAt": "2026-09-02T10:35:00.000Z"
  },
  "timestamp": "2026-09-02T10:35:00.000Z"
}
```

**Error (400) - Validation Failed:**
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
      "message": "Price (395) must be less than or equal to MRP (440)"
    }
  ],
  "timestamp": "2026-09-02T10:35:00.000Z"
}
```

---

### 2.4 PUT /api/seller/products/{id}

**URL:** `http://localhost:9000/api/seller/products/sp_102`  
**Method:** PUT  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Path Params:**
- `id` = sp_102

**Request Body (JSON):**
```json
{
  "title": "Ultratech Super Cement 50kg Bag - Updated",
  "description": "Updated description with enhanced features",
  "sellingPrice": 410,
  "mrp": 450,
  "moq": 40,
  "stockQty": 350,
  "bulkPricingTiers": [
    {
      "minQty": 40,
      "maxQty": 199,
      "price": 410,
      "discount": 0
    }
  ],
  "specifications": {
    "Grade": "PPC Grade",
    "Packaging": "50 kg HDPE Bag"
  }
}
```

**Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Product updated successfully",
  "data": {
    "id": "sp_102",
    "title": "Ultratech Super Cement 50kg Bag - Updated",
    "updatedAt": "2026-09-02T10:40:00.000Z"
  },
  "timestamp": "2026-09-02T10:40:00.000Z"
}
```

---

### 2.5 PATCH /api/seller/products/{id}/stock

**URL:** `http://localhost:9000/api/seller/products/sp_102/stock`  
**Method:** PATCH  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Path Params:**
- `id` = sp_102

**Request Body (JSON):**
```json
{
  "stockQty": 500
}
```

**Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Stock updated successfully",
  "data": {
    "id": "sp_102",
    "stockQty": 500,
    "updatedAt": "2026-09-02T10:45:00.000Z"
  },
  "timestamp": "2026-09-02T10:45:00.000Z"
}
```

---

### 2.6 PATCH /api/seller/products/{id}/pricing

**URL:** `http://localhost:9000/api/seller/products/sp_102/pricing`  
**Method:** PATCH  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Path Params:**
- `id` = sp_102

**Request Body (JSON):**
```json
{
  "sellingPrice": 390,
  "mrp": 440,
  "bulkPricingTiers": [
    {
      "minQty": 50,
      "maxQty": 199,
      "price": 390,
      "discount": 0
    },
    {
      "minQty": 200,
      "maxQty": 1000,
      "price": 375,
      "discount": 3.8
    }
  ]
}
```

**Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Product pricing updated successfully",
  "data": {
    "id": "sp_102",
    "sellingPrice": 390,
    "mrp": 440,
    "updatedAt": "2026-09-02T10:50:00.000Z"
  },
  "timestamp": "2026-09-02T10:50:00.000Z"
}
```

---

### 2.7 DELETE /api/seller/products/{id}

**URL:** `http://localhost:9000/api/seller/products/sp_102`  
**Method:** DELETE  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Path Params:**
- `id` = sp_102

**Response (200):**
```json
{
  "success": true,
  "statusCode": 200,
  "message": "Product removed from inventory",
  "data": {
    "id": "sp_102",
    "deletedAt": "2026-09-02T10:55:00.000Z"
  },
  "timestamp": "2026-09-02T10:55:00.000Z"
}
```

---

## 3. Warehouse Operations APIs

### 3.1 GET /api/seller/warehouses

**URL:** `http://localhost:9000/api/seller/warehouses`  
**Method:** GET  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Response (200):**
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

### 3.2 POST /api/seller/warehouses

**URL:** `http://localhost:9000/api/seller/warehouses`  
**Method:** POST  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (JSON):**
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

**Response (201):**
```json
{
  "success": true,
  "statusCode": 201,
  "message": "Warehouse added successfully",
  "data": {
    "id": "wh_2",
    "name": "Chakan Industrial Depot",
    "status": "Active",
    "createdAt": "2026-09-02T11:00:00.000Z"
  },
  "timestamp": "2026-09-02T11:00:00.000Z"
}
```

---

### 3.3 POST /api/seller/inventory/adjust

**URL:** `http://localhost:9000/api/seller/inventory/adjust`  
**Method:** POST  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (JSON):**
```json
{
  "productId": "sp_101",
  "warehouseId": "wh_1",
  "adjustmentType": "add",
  "quantity": 25,
  "reason": "New mill batch received from Tata Steel plant"
}
```

**Response (200):**
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
    "adjustedAt": "2026-09-02T11:05:00.000Z"
  },
  "timestamp": "2026-09-02T11:05:00.000Z"
}
```

---

## 4. Bulk Price Adjustments API

### 4.1 POST /api/seller/pricing/bulk-adjust

**URL:** `http://localhost:9000/api/seller/pricing/bulk-adjust`  
**Method:** POST  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (JSON) - By Category:**
```json
{
  "categoryId": 1,
  "brandId": null,
  "adjustmentType": "percentage_increase",
  "value": 3.5,
  "applyTo": "selling_price"
}
```

**Request Body (JSON) - By Brand:**
```json
{
  "categoryId": null,
  "brandId": 1,
  "adjustmentType": "fixed_increase",
  "value": 500,
  "applyTo": "both"
}
```

**Response (200):**
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
  "timestamp": "2026-09-02T11:10:00.000Z"
}
```

---

## 5. Document Vault APIs

### 5.1 GET /api/seller/documents

**URL:** `http://localhost:9000/api/seller/documents`  
**Method:** GET  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Response (200):**
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

### 5.2 POST /api/seller/documents

**URL:** `http://localhost:9000/api/seller/documents`  
**Method:** POST  
**Auth:** ✅ Required (Bearer Token)  
**Content-Type:** `multipart/form-data`

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Form Data:**
- `documentType` = GSTIN | PAN | INCORPORATION | MSME | TRADE_LICENSE
- `file` = (Binary file - PDF, JPEG, or PNG, max 5MB)

**Response (201):**
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
    "uploadedAt": "2026-09-02T11:15:00.000Z"
  },
  "timestamp": "2026-09-02T11:15:00.000Z"
}
```

---

## 6. Buyer Enquiries & Quotations APIs

### 6.1 GET /api/seller/enquiries

**URL:** `http://localhost:9000/api/seller/enquiries?status=NEW&page=1&limit=12`  
**Method:** GET  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Query Params:**
- `status` (optional) = NEW | QUOTED | ACCEPTED | REJECTED
- `sortBy` (optional) = newest | oldest
- `page` (optional) = 1
- `limit` (optional) = 12

**Response (200):**
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
      "createdAt": "2026-09-02T14:30:00.000Z"
    }
  ],
  "pagination": {
    "total": 1,
    "page": 1,
    "limit": 12,
    "totalPages": 1
  },
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

### 6.2 POST /api/seller/quotations

**URL:** `http://localhost:9000/api/seller/quotations`  
**Method:** POST  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body (JSON):**
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

**Response (201):**
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
    "sentAt": "2026-09-02T11:20:00.000Z",
    "emailSentAt": "2026-09-02T11:20:15.000Z"
  },
  "timestamp": "2026-09-02T11:20:00.000Z"
}
```

---

### 6.3 GET /api/seller/quotations

**URL:** `http://localhost:9000/api/seller/quotations?status=SENT&page=1&limit=12`  
**Method:** GET  
**Auth:** ✅ Required (Bearer Token)  
**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```
**Query Params:**
- `status` (optional) = SENT | VIEWED | ACCEPTED | EXPIRED | CONVERTED_TO_ORDER
- `sortBy` (optional) = newest | oldest
- `page` (optional) = 1
- `limit` (optional) = 12

**Response (200):**
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
      "sentAt": "2026-09-02T11:20:00.000Z",
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
  "timestamp": "2026-09-02T10:30:00.000Z"
}
```

---

## Postman Environment Variables

Create a Postman environment with these variables:

```json
{
  "name": "HinchMart Dev",
  "values": [
    {
      "key": "BASE_URL",
      "value": "http://localhost:9000",
      "enabled": true
    },
    {
      "key": "JWT_TOKEN",
      "value": "your_jwt_token_here",
      "enabled": true
    },
    {
      "key": "SELLER_ID",
      "value": "seller_1001",
      "enabled": true
    },
    {
      "key": "PRODUCT_ID",
      "value": "sp_101",
      "enabled": true
    },
    {
      "key": "WAREHOUSE_ID",
      "value": "wh_1",
      "enabled": true
    },
    {
      "key": "CATEGORY_ID",
      "value": "1",
      "enabled": true
    },
    {
      "key": "SUBCATEGORY_ID",
      "value": "1",
      "enabled": true
    },
    {
      "key": "BRAND_ID",
      "value": "1",
      "enabled": true
    }
  ]
}
```

---

## Usage in Postman

1. **Import Environment:** Use the environment JSON above
2. **Use Variables:** Replace hardcoded values with:
   - `{{BASE_URL}}` instead of `http://localhost:9000`
   - `{{JWT_TOKEN}}` in Authorization header
   - `{{PRODUCT_ID}}`, `{{WAREHOUSE_ID}}`, etc. in paths and query params

3. **Example Request URL:**
   ```
   {{BASE_URL}}/api/seller/products/{{PRODUCT_ID}}
   ```

4. **Example Authorization Header:**
   ```
   Authorization: Bearer {{JWT_TOKEN}}
   ```

---

## Error Response Examples

### 401 Unauthorized
```json
{
  "success": false,
  "statusCode": 401,
  "message": "Authentication required. Please provide a valid JWT token.",
  "timestamp": "2026-09-02T11:25:00.000Z"
}
```

### 403 Forbidden
```json
{
  "success": false,
  "statusCode": 403,
  "message": "Access denied. This product is not owned by you.",
  "timestamp": "2026-09-02T11:25:00.000Z"
}
```

### 404 Not Found
```json
{
  "success": false,
  "statusCode": 404,
  "message": "Product not found.",
  "timestamp": "2026-09-02T11:25:00.000Z"
}
```

### 409 Conflict
```json
{
  "success": false,
  "statusCode": 409,
  "message": "Duplicate SKU. This SKU already exists in your inventory.",
  "timestamp": "2026-09-02T11:25:00.000Z"
}
```

---

## Testing Order

1. **Categories** (No auth needed)
   - GET /api/categories
   - GET /api/subcategories?categoryId=1
   - GET /api/brands?subcategoryId=1

2. **Products** (Auth required)
   - POST /api/seller/products (Create)
   - GET /api/seller/products (List)
   - GET /api/seller/products/{id} (Get one)
   - PUT /api/seller/products/{id} (Update)
   - PATCH /api/seller/products/{id}/stock (Stock)
   - PATCH /api/seller/products/{id}/pricing (Pricing)
   - DELETE /api/seller/products/{id} (Delete)

3. **Warehouses** (Auth required)
   - GET /api/seller/warehouses
   - POST /api/seller/warehouses
   - POST /api/seller/inventory/adjust

4. **Pricing** (Auth required)
   - POST /api/seller/pricing/bulk-adjust

5. **Documents** (Auth required)
   - GET /api/seller/documents
   - POST /api/seller/documents

6. **Enquiries & Quotations** (Auth required)
   - GET /api/seller/enquiries
   - POST /api/seller/quotations
   - GET /api/seller/quotations

---

**Created:** 2026-09-02  
**Port:** 9000  
**Base URL:** http://localhost:9000
