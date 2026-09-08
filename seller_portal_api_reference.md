# 🛒 HinchMart Seller Portal — Frontend API Reference

**Base URL:** `http://localhost:9000`  
**Auth Header:** `X-Seller-Id: 1001` (pass on every `/api/seller/*` request)  
**Content-Type:** `application/json`

---

## Quick Overview

| # | Module | Endpoints | Purpose |
|---|--------|-----------|---------|
| 1 | Category Hierarchy | 3 GET | Cascading dropdowns in Add Product form |
| 2 | Product Management | 7 endpoints | Full product CRUD for the seller |
| 3 | Warehouse & Inventory | 3 endpoints | Manage warehouses + stock adjustments |
| 4 | Bulk Pricing | 1 POST | Mass price/MRP update across products |
| 5 | Document Vault | 2 endpoints | Upload & track KYC documents |
| 6 | Enquiries & Quotations | 3 endpoints | Respond to buyer RFQs |

---

## Module 1 — Category → Subcategory → Brand Cascade

> **Use case:** The "Add Product" form has 3 dependent dropdowns. When the user picks a Category, load Subcategories. When they pick a Subcategory, load Brands.

### 1.1 Get All Categories

```
GET /api/categories
```

**When to call:** On page load of the Add Product form.

**Response:**
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
      "imageUrl": "https://cdn.hinchmart.com/categories/civil_structural.jpg",
      "sortOrder": 1,
      "productCount": 2,
      "active": true
    }
  ]
}
```

**Frontend usage:**
```js
const res = await fetch('/api/categories');
const { data } = await res.json();
// Populate first dropdown with data.map(c => ({ value: c.id, label: c.name }))
```

---

### 1.2 Get Subcategories by Category

```
GET /api/subcategories?categoryId={categoryId}
```

**When to call:** When the user selects a category from the first dropdown.

**Example:** `GET /api/subcategories?categoryId=1`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "subcategoryId": 1,
      "categoryId": 1,
      "name": "TMT Steel & Rebars",
      "slug": "tmt-steel-rebars",
      "imageUrl": "https://cdn.hinchmart.com/subcategories/tmt_steel.jpg",
      "active": true
    }
  ]
}
```

**Frontend usage:**
```js
const res = await fetch(`/api/subcategories?categoryId=${selectedCategoryId}`);
const { data } = await res.json();
// Populate second dropdown, reset third dropdown (brands)
```

---

### 1.3 Get Brands by Subcategory

```
GET /api/brands?subcategoryId={subcategoryId}
```

**When to call:** When the user selects a subcategory.

**Example:** `GET /api/brands?subcategoryId=1`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "brandId": 1,
      "subcategoryId": 1,
      "name": "Tata Tiscon",
      "slug": "tata-tiscon",
      "logoUrl": "https://cdn.hinchmart.com/brands/tata_tiscon.png",
      "active": true
    }
  ]
}
```

---

## Module 2 — Seller Product Management

> **Use case:** The seller's "My Products" page — list, create, edit, delete products, and quick-update stock or pricing.

### 2.1 List Seller's Products (Paginated)

```
GET /api/seller/products?page=1&limit=12&status=APPROVED&search=cement
```

**Headers:** `X-Seller-Id: 1001`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 1 | Page number |
| `limit` | int | 12 | Items per page |
| `status` | string | *(all)* | Filter: `PENDING`, `APPROVED`, `REJECTED` |
| `search` | string | *(none)* | Search by product title |

**Response:**
```json
{
  "success": true,
  "total": 1,
  "page": 1,
  "limit": 12,
  "data": [
    {
      "id": 5,
      "productId": 5,
      "sellerId": "seller_1001",
      "title": "Ultratech Super Cement 50kg Bag",
      "sku": "SKU-ULTRA-PPC-50KG",
      "description": "Engineered PPC cement...",
      "categoryId": 1,
      "categoryName": "Civil & Structural",
      "subcategoryId": 2,
      "subcategoryName": "Cement & RMC",
      "brandId": 1,
      "brand": "Tata Tiscon",
      "price": 395.00,
      "sellingPrice": 395.00,
      "mrp": 440.00,
      "unit": "Bags",
      "moq": 50,
      "stockQty": 400,
      "gstRate": 18.00,
      "status": "PENDING",
      "approvalStatus": "PENDING",
      "is24HourDelivery": true,
      "active": true,
      "images": [],
      "createdAt": "2026-09-02T12:06:39",
      "updatedAt": "2026-09-02T12:06:39"
    }
  ]
}
```

**Frontend usage:**
```js
const res = await fetch('/api/seller/products?page=1&limit=12', {
  headers: { 'X-Seller-Id': '1001' }
});
const { data, total, page, limit } = await res.json();
// Render product table/grid + pagination
```

---

### 2.2 Get Single Product

```
GET /api/seller/products/{productId}
```

**Headers:** `X-Seller-Id: 1001`  
**When to call:** When opening the "Edit Product" form.

**Example:** `GET /api/seller/products/5`

**Response:** Same shape as a single item from the list above.

---

### 2.3 Create a New Product

```
POST /api/seller/products
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Request Body:**
```json
{
  "title": "Ultratech Super Cement 50kg Bag",
  "sku": "SKU-ULTRA-PPC-50KG",
  "description": "Engineered PPC cement designed for superior compressive strength.",
  "categoryId": 1,
  "subcategoryId": 2,
  "brandId": 1,
  "price": 395,
  "sellingPrice": 395,
  "mrp": 440,
  "unit": "Bags",
  "moq": 50,
  "stockQty": 400,
  "is24HourDelivery": true
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `title` | ✅ | Product name |
| `sku` | ✅ | Unique stock-keeping unit code |
| `description` | ❌ | Product description |
| `categoryId` | ✅ | From categories dropdown |
| `subcategoryId` | ✅ | From subcategories dropdown |
| `brandId` | ✅ | From brands dropdown |
| `price` | ✅ | Seller's base price |
| `sellingPrice` | ❌ | Selling price (defaults to `price`) |
| `mrp` | ✅ | Maximum retail price |
| `unit` | ✅ | e.g. `"Bags"`, `"MT"`, `"Meters"` |
| `moq` | ✅ | Minimum order quantity |
| `stockQty` | ✅ | Available stock |
| `is24HourDelivery` | ❌ | `true`/`false` |

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Product submitted successfully for admin review.",
  "data": {
    "id": "sp_5",
    "productId": "sp_5",
    "sellerId": "seller_1001",
    "title": "Ultratech Super Cement 50kg Bag",
    "sku": "SKU-ULTRA-PPC-50KG",
    "status": "PENDING",
    "createdAt": "2026-09-02T12:06:39"
  }
}
```

> [!NOTE]
> New products start in `PENDING` status and require admin approval before they appear to buyers.

---

### 2.4 Update a Product

```
PUT /api/seller/products/{productId}
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Request Body:** Same fields as create (all optional — only send fields you want to update).

```json
{
  "title": "Ultratech Super Cement 50kg (Updated)",
  "price": 405,
  "stockQty": 350
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Product updated successfully.",
  "data": { "id": "sp_5", "title": "...", "status": "PENDING" }
}
```

---

### 2.5 Delete a Product

```
DELETE /api/seller/products/{productId}
```

**Headers:** `X-Seller-Id: 1001`

**Response (200):**
```json
{
  "success": true,
  "message": "Product deleted successfully."
}
```

---

### 2.6 Quick Stock Update (Patch)

```
PATCH /api/seller/products/{productId}/stock
```

**Use case:** Inline "Update Stock" button on the product list — no need to open full edit form.

**Request Body:**
```json
{
  "stockQty": 500,
  "reason": "New shipment received"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Stock updated.",
  "data": { "productId": 5, "stockQty": 500 }
}
```

---

### 2.7 Quick Pricing Update (Patch)

```
PATCH /api/seller/products/{productId}/pricing
```

**Use case:** Inline "Update Price" button — update price/MRP without full edit.

**Request Body:**
```json
{
  "price": 410,
  "sellingPrice": 410,
  "mrp": 450
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Pricing updated.",
  "data": { "productId": 5, "price": 410, "sellingPrice": 410, "mrp": 450 }
}
```

---

## Module 3 — Warehouse & Inventory

> **Use case:** "Warehouses" page where sellers manage storage locations and adjust stock with audit trails.

### 3.1 List Seller's Warehouses

```
GET /api/seller/warehouses
```

**Headers:** `X-Seller-Id: 1001`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "wh_1",
      "warehouseId": 1,
      "sellerId": 1001,
      "name": "Bhiwandi Central Logistics Yard",
      "contactPerson": "Logistics Manager",
      "phone": "+91 98201 11223",
      "city": "Bhiwandi",
      "state": "Maharashtra",
      "pincode": "421302",
      "address": "Plot C-14, Mankoli Logistics Hub, Bhiwandi",
      "capacityTons": 15000,
      "status": "Active",
      "isDefault": true,
      "createdAt": "2026-09-02T12:06:31",
      "updatedAt": "2026-09-02T12:06:31"
    }
  ]
}
```

> [!TIP]
> If no warehouses exist, the API automatically creates a default one. The frontend can always expect at least 1 warehouse.

---

### 3.2 Create a Warehouse

```
POST /api/seller/warehouses
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Request Body:**
```json
{
  "name": "Navi Mumbai Distribution Center",
  "contactPerson": "Suresh Kumar",
  "phone": "+91 98765 43210",
  "city": "Navi Mumbai",
  "state": "Maharashtra",
  "pincode": "400705",
  "address": "Unit 7, TTC Industrial Area, Turbhe MIDC",
  "capacityTons": 8000,
  "isDefault": false
}
```

**Response (201):** Returns the created warehouse object.

---

### 3.3 Adjust Inventory (Stock In / Stock Out)

```
POST /api/seller/inventory/adjust
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Use case:** Record stock additions (new shipment) or deductions (damaged goods) with an audit trail.

**Request Body:**
```json
{
  "productId": 5,
  "warehouseId": 1,
  "adjustmentType": "IN",
  "quantity": 200,
  "reason": "New shipment from supplier"
}
```

| Field | Values | Description |
|-------|--------|-------------|
| `adjustmentType` | `"IN"` or `"OUT"` | Stock increase or decrease |
| `quantity` | positive int | Number of units |
| `reason` | string | Audit reason (displayed in history) |

**Response (200):**
```json
{
  "success": true,
  "message": "Inventory adjusted: +200 units for product 5",
  "data": {
    "adjustmentId": 1,
    "productId": 5,
    "previousStock": 400,
    "newStock": 600,
    "adjustmentType": "IN",
    "quantity": 200
  }
}
```

---

## Module 4 — Bulk Price Adjustment

> **Use case:** "Bulk Actions" page — seller wants to increase all prices by 5% or set a fixed MRP across multiple products.

### 4.1 Bulk Adjust Prices

```
POST /api/seller/pricing/bulk-adjust
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Request Body (Percentage adjustment):**
```json
{
  "productIds": [5, 6, 7],
  "adjustmentType": "PERCENTAGE",
  "adjustmentValue": 5.0,
  "applyTo": "PRICE"
}
```

**Request Body (Fixed value):**
```json
{
  "productIds": [5, 6],
  "adjustmentType": "FIXED",
  "adjustmentValue": 500.00,
  "applyTo": "MRP"
}
```

| Field | Values | Description |
|-------|--------|-------------|
| `productIds` | `[1, 2, 3]` | Array of product IDs to update |
| `adjustmentType` | `"PERCENTAGE"` or `"FIXED"` | How to apply the change |
| `adjustmentValue` | number | % increase or fixed new value |
| `applyTo` | `"PRICE"` or `"MRP"` | Which field to adjust |

**Response (200):**
```json
{
  "success": true,
  "message": "Bulk pricing adjusted for 3 products.",
  "data": {
    "totalUpdated": 3,
    "adjustmentType": "PERCENTAGE",
    "adjustmentValue": 5.0,
    "applyTo": "PRICE"
  }
}
```

---

## Module 5 — Document Vault (KYC)

> **Use case:** "Documents" page where sellers upload and track verification documents.

### 5.1 List All Documents

```
GET /api/seller/documents
```

**Headers:** `X-Seller-Id: 1001`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "documentType": "GSTIN",
      "label": "GSTIN Certificate",
      "fileName": "gstin_cert.pdf",
      "fileUrl": "https://storage.example.com/docs/gstin_cert.pdf",
      "status": "VERIFIED",
      "uploadedAt": "2026-08-15T10:30:00",
      "verifiedAt": "2026-08-16T14:00:00"
    },
    {
      "documentType": "PAN",
      "label": "PAN Card",
      "fileName": null,
      "fileUrl": null,
      "status": "NOT_UPLOADED",
      "uploadedAt": null,
      "verifiedAt": null
    }
  ]
}
```

**Frontend usage:**
```js
// Show all 5 document types with their status
// Status badges: NOT_UPLOADED (grey), UPLOADED (yellow), VERIFIED (green), REJECTED (red)
```

| `documentType` | Label |
|----------------|-------|
| `GSTIN` | GSTIN Certificate |
| `PAN` | PAN Card |
| `INCORPORATION` | Certificate of Incorporation |
| `MSME` | MSME/Udyam Registration |
| `TRADE_LICENSE` | Trade License |

---

### 5.2 Upload a Document

```
POST /api/seller/documents
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Request Body:**
```json
{
  "documentType": "PAN",
  "fileName": "pan_card_scan.pdf",
  "fileUrl": "https://storage.example.com/uploads/pan_card_scan.pdf"
}
```

> [!IMPORTANT]
> Upload the file to your storage (S3/Firebase) first, then send the `fileUrl` to this API. This endpoint does NOT accept file uploads directly — it saves the reference URL.

**Response (200):**
```json
{
  "success": true,
  "message": "Document 'PAN' uploaded successfully. Pending verification.",
  "data": {
    "documentType": "PAN",
    "status": "UPLOADED",
    "fileName": "pan_card_scan.pdf"
  }
}
```

---

## Module 6 — Buyer Enquiries & Quotations

> **Use case:** "Enquiries" page where sellers see buyer RFQ (Request for Quote) messages and can respond with formal quotations.

### 6.1 List Buyer Enquiries

```
GET /api/seller/enquiries
```

**Headers:** `X-Seller-Id: 1001`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "enquiryId": 1,
      "buyerName": "Ravi Construction Pvt Ltd",
      "buyerEmail": "procurement@raviconstruction.com",
      "buyerPhone": "+91 98765 12345",
      "productTitle": "Ultratech Super Cement 50kg Bag",
      "productId": 5,
      "quantity": 500,
      "message": "Need bulk pricing for 500 bags, delivery to Pune site.",
      "status": "OPEN",
      "createdAt": "2026-09-01T14:30:00"
    }
  ]
}
```

**Frontend status badges:**
- `OPEN` → New enquiry (blue)
- `QUOTED` → Quotation sent (yellow)
- `CLOSED` → Completed (green)

---

### 6.2 Create a Quotation (Reply to Enquiry)

```
POST /api/seller/quotations
```

**Headers:** `X-Seller-Id: 1001`, `Content-Type: application/json`

**Request Body:**
```json
{
  "enquiryId": 1,
  "buyerName": "Ravi Construction Pvt Ltd",
  "buyerEmail": "procurement@raviconstruction.com",
  "buyerPhone": "+91 98765 12345",
  "validityDays": 15,
  "freightCharge": 2500.00,
  "notes": "Delivery within 3 working days to Pune.",
  "items": [
    {
      "productId": 5,
      "productTitle": "Ultratech Super Cement 50kg Bag",
      "quantity": 500,
      "unitPrice": 380.00,
      "gstRate": 18.0
    }
  ]
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "Quotation QTN-20260902-XXXX created successfully.",
  "data": {
    "quotationId": 1,
    "quotationNumber": "QTN-20260902-XXXX",
    "buyerName": "Ravi Construction Pvt Ltd",
    "subtotal": 190000.00,
    "gstAmount": 34200.00,
    "freightCharge": 2500.00,
    "grandTotal": 226700.00,
    "validUntil": "2026-09-17",
    "status": "SENT",
    "createdAt": "2026-09-02T12:30:00"
  }
}
```

> [!TIP]
> The backend automatically calculates `subtotal`, `gstAmount`, and `grandTotal`. The frontend only needs to send `unitPrice`, `quantity`, `gstRate`, and `freightCharge`.

---

### 6.3 List Sent Quotations

```
GET /api/seller/quotations
```

**Headers:** `X-Seller-Id: 1001`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "quotationId": 1,
      "quotationNumber": "QTN-20260902-XXXX",
      "buyerName": "Ravi Construction Pvt Ltd",
      "subtotal": 190000.00,
      "gstAmount": 34200.00,
      "freightCharge": 2500.00,
      "grandTotal": 226700.00,
      "validUntil": "2026-09-17",
      "status": "SENT",
      "createdAt": "2026-09-02T12:30:00"
    }
  ]
}
```

---

## 🔗 Frontend Integration Cheat Sheet

### Headers to send on every seller API call:
```js
const SELLER_HEADERS = {
  'Content-Type': 'application/json',
  'X-Seller-Id': '1001'   // Replace with logged-in seller's ID
};
```

### API Helper Example (React/Next.js):
```js
const API_BASE = 'http://localhost:9000';

async function sellerApi(endpoint, options = {}) {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Seller-Id': getLoggedInSellerId(),  // from auth context
      ...options.headers,
    },
  });
  return res.json();
}

// Usage examples:
const categories  = await sellerApi('/api/categories');
const products    = await sellerApi('/api/seller/products?page=1&limit=12');
const newProduct  = await sellerApi('/api/seller/products', {
  method: 'POST',
  body: JSON.stringify({ title: '...', sku: '...', /* ... */ }),
});
```

### Page → API Mapping:

| Frontend Page | APIs Used |
|---------------|-----------|
| **Add Product** | `GET /api/categories` → `GET /api/subcategories` → `GET /api/brands` → `POST /api/seller/products` |
| **My Products** | `GET /api/seller/products` + `PATCH .../stock` + `PATCH .../pricing` + `DELETE` |
| **Edit Product** | `GET /api/seller/products/{id}` → `PUT /api/seller/products/{id}` |
| **Warehouses** | `GET /api/seller/warehouses` + `POST /api/seller/warehouses` |
| **Inventory** | `POST /api/seller/inventory/adjust` |
| **Bulk Pricing** | `POST /api/seller/pricing/bulk-adjust` |
| **Documents** | `GET /api/seller/documents` + `POST /api/seller/documents` |
| **Enquiries** | `GET /api/seller/enquiries` |
| **Quotations** | `POST /api/seller/quotations` + `GET /api/seller/quotations` |
