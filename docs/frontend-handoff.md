# HinchMart Backend Frontend Handoff Specification

**Target Audience:** Frontend Engineering Team (Customer Web/Mobile, Seller Portal, Admin Console)  
**Scope:** Backend API behavior across Phases 0–3, Phase 3 Step 4 Consignment Inventory & Ledger Work, and Multi-Store Marketplace Architecture.  
**Constraint Verification Notice:** All endpoints, DTO class names, field mappings, HTTP statuses, and business rules documented below have been verified directly against active Spring Boot source code in `CustomerBackend`. Features not present in backend code are explicitly flagged as `(unverified / not present in backend)`.

---

## 1. Roles and Authentication

### 1.1 Role Model and Identity Resolution
HinchMart supports three primary roles defined in the enum `com.example.project.customer.entity.Role`:
- `CUSTOMER` (synonym: `BUYER` via Spring Security authority `ROLE_BUYER` and `ROLE_CUSTOMER`)
- `SELLER` (`ROLE_SELLER`)
- `ADMIN` (`ROLE_ADMIN`)

Authentication is token-based using HinchMart-issued JWTs passed in the standard HTTP header:
```http
Authorization: Bearer <accessToken>
```

#### How the Principal and Role are Resolved in Code
Identity and roles are resolved per-request by `com.example.project.customer.security.JwtAuthenticationFilter`:
1. **Public Exclusion:** Requests to `POST /api/auth/sync` and `GET /api/auth/check-phone` bypass JWT inspection.
2. **JWT Claims Extraction:** The filter extracts claims and `customerId` from the Bearer token via `JwtService`.
3. **Database Customer Lookup:** The customer entity is loaded from `CustomerRepository`. If missing, the security context is cleared.
4. **Role Resolution Hierarchy (`resolveEffectiveRole` in `JwtAuthenticationFilter`):**
   - **Step 1:** If JWT claims contain `role: "ADMIN"` or `admin: true`, role resolves to `Role.ADMIN`.
   - **Step 2:** If `AdminUserRepository` contains customer email or Firebase UID, or `customer.getRole() == "ADMIN"`, role resolves to `Role.ADMIN`.
   - **Step 3:** If customer email matches configured admin emails (`${app.security.admin-emails}` or `admin@hinchmart.com`), role resolves to `Role.ADMIN`.
   - **Step 4:** If claims contain `role: "SELLER"`, role resolves to `Role.SELLER`.
   - **Step 5:** If `customer.getRoleEnum() == Role.SELLER`, role resolves to `Role.SELLER`.
   - **Step 6:** If `UserService.resolveSellerIdForUser(customer)` finds an associated seller profile, role resolves to `Role.SELLER`.
   - **Step 7:** Defaults to `customer.getRoleEnum()` or `Role.CUSTOMER`.
5. **Security Principal Construction:** A `FirebaseUserPrincipal` is created and stored in `SecurityContextHolder`:
   - `Role.CUSTOMER`: Grants authorities `ROLE_CUSTOMER` and `ROLE_BUYER`.
   - `Role.SELLER`: Grants authority `ROLE_SELLER`.
   - `Role.ADMIN`: Grants authority `ROLE_ADMIN`.

---

### 1.2 Authentication Endpoints

#### Pre-Login Phone Check
- **Method & Path:** `GET /api/auth/check-phone?phone={phone}`
- **Required Role:** Public (`permitAll`)
- **Controller:** `com.example.project.customer.controller.AuthController`
- **Request Parameters:** `phone` (`String`, required)
- **Response Body (`ApiResponse<CheckPhoneResponse>`):**
  - `exists` (`boolean`): Whether phone is already registered.
  - `role` (`String`): Role assigned to this phone (`CUSTOMER`, `SELLER`, `ADMIN`).
  - `name` (`String`): Registered name if exists.
  - `message` (`String`): Status description.
- **Error Responses:**
  - `400 Bad Request` if phone number is blank or invalid.

#### Firebase ID Token Exchange / User Sync
- **Method & Path:** `POST /api/auth/sync`
- **Required Role:** Public (`permitAll`)
- **Controller:** `com.example.project.customer.controller.AuthController`
- **Request Body DTO (`AuthSyncRequest`):**
  - `firebaseIdToken` (`String`, required)
  - `email` (`String`, optional)
  - `name` (`String`, optional)
  - `phone` (`String`, optional)
  - `role` (`String`, optional)
- **Response Body (`ApiResponse<AuthUserResponse>`):**
  - `accessToken` (`String`): Backend JWT to include in subsequent `Authorization: Bearer <token>` requests.
  - `tokenType` (`String`): `"Bearer"`
  - `expiresIn` (`Long`): Token validity in seconds.
  - `customer` (`CustomerResponse`): Profile object (`customerId`, `name`, `email`, `phone`, `role`, etc.).
- **Error Responses:**
  - `400 Bad Request`: Missing `firebaseIdToken`.
  - `401 Unauthorized`: Firebase token verification failure.

---

### 1.3 Authentication & Authorization Error Responses (401 vs 403)

All backend error responses strictly conform to `com.example.project.customer.dto.ApiResponse<T>`:

#### HTTP 401 Unauthorized
Triggered by `com.example.project.customer.security.FirebaseAuthEntryPoint` when JWT is missing, invalid, or expired:
```json
{
  "statusCode": 401,
  "message": "Unauthorized: Authentication token is missing, invalid, or expired.",
  "data": null,
  "errors": null
}
```

#### HTTP 403 Forbidden
Triggered by `com.example.project.customer.security.FirebaseAccessDeniedHandler` when caller lacks the required role:
```json
{
  "statusCode": 403,
  "message": "Forbidden: You do not have permission to access this resource.",
  "data": null,
  "errors": null
}
```
*Business Logic 403 Example:* When a seller attempts to mark an order as `DELIVERED`, `OrderServiceImpl` throws `ForbiddenException`, handled by `GlobalExceptionHandler.handleForbidden`:
```json
{
  "statusCode": 403,
  "message": "Access denied: Only administrators can mark an order as DELIVERED.",
  "data": null,
  "errors": null
}
```

---

## 2. CUSTOMER

### 2.1 Product Catalog & Availability

#### Product List
- **Method & Path:** `GET /api/products`
- **Required Role:** Public (`permitAll`)
- **Controller:** `com.example.project.customer.controller.ProductController`
- **Query Parameters:** `categoryId` (`Integer`), `category` (`String`), `subcategoryId` (`Integer`), `brandId` (`Integer`), `brand` (`String`), `search` (`String`), `minPrice` (`BigDecimal`), `maxPrice` (`BigDecimal`), `is24HourDelivery` (`Boolean`), `sort` (`String`, default `"newest"`), `page` (`int`, default 1), `limit` (`int`, default 20).
- **Response Type:** `ApiResponse<List<ProductResponse>>`
- **Availability Fields on `ProductResponse`:**
  - `stockQty` (`Integer`): Raw physical stock from `products` table.
  - `availableQty` (`Integer`): Usable availability after consignment / shared stock calculation.
  - `inStock` (`Boolean`): `true` if `availableQty > 0`, otherwise `false`.
  - `availabilityReason` (`String`): Reason code (`AVAILABLE`, `OUT_OF_STOCK`, `CHECKOUT_DISABLED`, `UNAVAILABLE_NO_VENDOR_STOCK`, `STORE_CLOSED`, `PRODUCT_INACTIVE`).

#### Product Detail
- **Method & Path:** `GET /api/products/{id}`
- **Required Role:** Public (`permitAll`)
- **Controller:** `ProductController`
- **Response Type:** `ApiResponse<ProductResponse>`
- **Availability Fields:** `stockQty`, `availableQty`, `inStock`, `availabilityReason` computed via `ProductAvailabilityService.getAvailability(product)`.
- **Error Responses:** `404 Not Found` if product does not exist.

#### Store Products
- **Method & Path:** `GET /api/stores/{slugOrId}/products`
- **Required Role:** Public (`permitAll`)
- **Controller:** `com.example.project.customer.controller.StoreController`
- **Query Parameters:** `search`, `categoryId`, `subcategoryId`, `page` (default 1), `limit` (default 20), `sortBy` (default `"featured"`).
- **Response Type:** `ApiResponse<List<ProductResponse>>`
- **Availability Fields:** Populated across all returned products.

#### Category & Brand Filtering
- **Category:** `GET /api/products?categoryId={id}` or `GET /api/products?category={slug}`
- **Brand:** `GET /api/products?brandId={id}` or `GET /api/products?brand={name}`
- **Response Type:** `ApiResponse<List<ProductResponse>>` with availability fields.

#### Product Search
- **Method & Path:** `GET /api/products?search={term}`
- **Required Role:** Public (`permitAll`)
- **Implementation Note:** Backend queries `ProductRepository.searchCatalog` using `LEFT JOIN` on consignment inventory so catalog items without consignment mappings are not omitted from search results.
- **Availability Fields:** Populated via batch availability service.

#### Hot Deals
- **Method & Path:** `GET /api/hot-deals`
- **Required Role:** Public (`permitAll`)
- **Controller:** `com.example.project.customer.controller.HomeHotDealController`
- **Response Type:** `ApiResponse<List<HotDealResponse>>`
- **Structure:** Each `HotDealResponse` contains `product: ProductResponse` (`productId`, `title`, `price`, `dealPrice`, `discountPercentage`, etc.).
- **Availability Behavior:** Batched via `ProductAvailabilityService.getBatchAvailability(...)` to eliminate N+1 queries. Each nested `product` includes `stockQty`, `availableQty`, `inStock`, and `availabilityReason`.

#### Wishlist
- **Method & Path:** `GET /api/wishlist`
- **Required Role:** Authenticated (`ROLE_CUSTOMER` / `ROLE_BUYER`)
- **Controller:** `com.example.project.customer.controller.WishlistController`
- **Response Type:** `ApiResponse<List<WishlistResponse>>`
- **Add to Wishlist:** `POST /api/wishlist/{productId}`
- **Remove from Wishlist:** `DELETE /api/wishlist/{productId}`
- **CRITICAL GOTCHA — Wishlist Availability Fields:**
  In `WishlistServiceImpl.mapToResponse(WishlistItem item)`, the nested `ProductResponse` is constructed directly from the entity:
  - `stockQty`: Populated from database entity (`product.getStockQty()`).
  - `availableQty`: **`null`** (not computed by `ProductAvailabilityService`).
  - `inStock`: **`null`** (not computed).
  - `availabilityReason`: **`null`** (not computed).
  *Frontend Recommendation:* Do NOT rely on `inStock` or `availableQty` from the wishlist response. When displaying stock badges on the wishlist page, fetch product details from `GET /api/products/{id}` or check availability before adding to cart.

---

### 2.2 Cart Operations & Stock Validation

The cart is single-store locked. An active cart can only contain items from one store at a time.

#### Cart Endpoints
- **Get Cart:** `GET /api/cart`
- **Add Item:** `POST /api/cart/items` — Body: `CartItemRequest` (`productId`: Long/Integer, `quantity`: Integer)
- **Update Item Quantity:** `PUT /api/cart/items/{id}` — Body: `CartItemUpdateRequest` (`quantity`: Integer)
- **Remove Item:** `DELETE /api/cart/items/{id}`
- **Clear Cart:** `DELETE /api/cart`
- **Switch Store:** `POST /api/cart/switch-store` — Body: `SwitchStoreRequest` (`storeId`: Integer, `storeSlug`: String, `pendingProductId`: Integer, `pendingQuantity`: Integer)
- **Apply Coupon:** `POST /api/cart/coupon` — Body: `CouponRequest` (`code`: String)
- **Remove Coupon:** `DELETE /api/cart/coupon`

#### DTO Definitions
- **`CartResponse`:**
  - `cartId` (`Integer`)
  - `customerId` (`Integer`)
  - `storeId` (`Integer`): Store ID to which the cart is currently locked.
  - `storeName` (`String`)
  - `storeSlug` (`String`)
  - `items` (`List<CartItemResponse>`)
  - `subtotal` (`BigDecimal`)
  - `taxAmount` (`BigDecimal`)
  - `shippingFee` (`BigDecimal`)
  - `discountAmount` (`BigDecimal`)
  - `totalAmount` (`BigDecimal`)
  - `couponCode` (`String`)
  - `currency` (`String`, default `"INR"`)
  - `itemCount` (`Integer`)

#### How Stock is Checked in Cart
When `POST /api/cart/items` is called:
1. Store active status is verified. If store is inactive/suspended, throws `IllegalStateException` (`400 Bad Request`).
2. `ProductAvailabilityService.getAvailability(product)` evaluates live stock:
   - For regular seller products: checks `product.stockQty`.
   - For HinchStore products (storeId 1): checks aggregated active vendor consignment inventory in `HinchStoreVendorInventory`.
3. If `reason == "CHECKOUT_DISABLED"`, throws `ResourceConflictException("Cannot add '<title>' to cart: CHECKOUT_DISABLED")` (`409 Conflict`).
4. If `reason == "UNAVAILABLE_NO_VENDOR_STOCK"`, throws `ResourceConflictException("Cannot add '<title>' to cart: UNAVAILABLE_NO_VENDOR_STOCK")` (`409 Conflict`).
5. If requested quantity exceeds `availableQty`, throws `ResourceConflictException("Cannot add '<title>' to cart. Requested quantity (<qty>) exceeds available stock (available: <available>).")` (`409 Conflict`).

#### Multi-Store Conflict (Store Mismatch)
If a customer attempts to add an item from Store B to an active cart locked to Store A, `CartServiceImpl` throws `StoreMismatchException`:
- **HTTP Status:** `409 Conflict`
- **Response Format:**
```json
{
  "status": 409,
  "error": "STORE_MISMATCH",
  "message": "Cart contains items from Store A. Switch store to continue?",
  "currentStore": {
    "id": 1,
    "name": "HinchStore Central",
    "slug": "hinchstore-central"
  },
  "newStore": {
    "id": 5,
    "name": "Apex Steel Works",
    "slug": "apex-steel-works"
  }
}
```
*Frontend Resolution:* Prompt user with modal to clear/switch store using `POST /api/cart/switch-store`.

---

### 2.3 Checkout Flow

#### Checkout Preview
- **Method & Path:** `POST /api/checkout/preview`
- **Required Role:** Authenticated (`ROLE_CUSTOMER` / `ROLE_BUYER`)
- **Controller:** `com.example.project.customer.controller.CheckoutController`
- **Important Path Gotcha:** The endpoint is strictly `POST /api/checkout/preview`. The path `/api/cart/preview` does **NOT** exist.
- **Request Body DTO (`CheckoutPreviewRequest`):**
  - `addressId` (`Long` or `Integer`, optional): Destination address for tax and delivery calculation.
  - `couponCode` (`String`, optional)
- **Response Body (`ApiResponse<CheckoutPreviewResponse>`):**
  - `items` (`List<CheckoutPreviewItemResponse>`): List of items with price and tax breakdowns.
  - `subtotal` (`BigDecimal`)
  - `taxAmount` (`BigDecimal`)
  - `cgst` (`BigDecimal`), `sgst` (`BigDecimal`), `igst` (`BigDecimal`)
  - `shippingFee` (`BigDecimal`)
  - `discountAmount` (`BigDecimal`)
  - `finalAmount` (`BigDecimal`)
  - `couponApplied` (`Boolean`)
  - `estimatedDeliveryDays` (`Integer`)
  - `availableStockChecked` (`Boolean`): `true` indicating all line items passed live availability validation.
- **Preview Stock Validation:**
  Every cart item is re-validated against `ProductAvailabilityService`. If any item's quantity exceeds current stock, throws `ResourceConflictException` (`409 Conflict`) with the item details.

#### Place Order
- **Method & Path:** `POST /api/orders` (or `POST /api/customer/orders`)
- **Order For Someone Else:** `POST /api/orders/for-someone-else`
- **Required Role:** Authenticated (`ROLE_CUSTOMER` / `ROLE_BUYER`)
- **Controller:** `com.example.project.customer.controller.OrderController`
- **Request Body DTO (`OrderCreateRequest`):**
  - `addressId` (`Integer`, required)
  - `deliveryInstructions` (`String`, optional)
  - `deliverySlot` (`String`, optional)
  - `poNumber` (`String`, optional)
  - `requiresCraneUnloading` (`Boolean`, optional)
  - `orderForSomeoneElse` (`Boolean`, optional)
  - `recipientName` (`String`, optional)
  - `recipientPhone` (`String`, optional)
  - `paymentMethod` (`String`, required): Supported values:
    - `"ONLINE"`: Initiates Razorpay payment gateway order.
    - `"CASH_ON_DELIVERY"` / `"COD"`
    - `"WALLET"`
    - `"CREDIT"`
- **Response Body (`ApiResponse<OrderResponse>`):**
  - Real DTO: `com.example.project.customer.dto.OrderResponse`
  - Fields: `orderId`, `orderNumber`, `customerId`, `storeId`, `storeName`, `fulfillmentStoreId`, `fulfillmentStoreName`, `inventoryState`, `totalAmount`, `subtotal`, `taxAmount`, `orderStatus`, `paymentStatus`, `paymentMethod`, `items`.

#### Inventory Deduction Logic:
1. **Regular Seller Orders:** Deducts directly from `Product.stockQty` atomically.
2. **HinchStore Consignment Orders (Store ID 1):**
   - Routes through `HinchStoreInventoryService` / `HinchStoreVendorInventoryRepository`.
   - Atomically reserves vendor inventory (`atomicReserve`).
   - Sets `order.inventoryState = "RESERVED"`.
   - Upon payment capture or delivery confirmation, transitions to `"COMMITTED"` via `commitHinchStoreReservation`.
3. **What Happens if HinchStore Checkout is Disabled?**
   If `platformMarketplaceProperties.isHinchStoreCheckoutEnabled() == false` (Phase 1 safety gate), `OrderServiceImpl` throws:
   ```java
   throw new IllegalStateException("HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.");
   ```
   **HTTP Status:** `GlobalExceptionHandler` maps `IllegalStateException` to **`HTTP 400 Bad Request`**:
   ```json
   {
     "statusCode": 400,
     "message": "HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout.",
     "data": null,
     "errors": null
   }
   ```

---

### 2.4 Customer Orders

#### Order List
- **Method & Path:** `GET /api/orders`
- **Query Parameters:** `status` (`String`, optional), `page` (`int`, default 1), `limit` (`int`, default 20)
- **Response Type:** `ApiResponse<List<OrderSummaryResponse>>`

#### Order Detail
- **Method & Path:** `GET /api/orders/{id}`
- **Response Type:** `ApiResponse<OrderResponse>`
- **Fulfillment Visibility:** `OrderResponse` includes both `storeId` (sales store, e.g. 1) and `fulfillmentStoreId` / `fulfillmentStoreName` (the consignment vendor store actually fulfilling the order).

#### Order Tracking Checkpoints
- **Method & Path:** `GET /api/orders/{id}/tracking`
- **Response Type:** `ApiResponse<OrderTrackingResponse>`

#### Tax Invoice Breakdown & PDF Download
- **Breakdown:** `GET /api/orders/{id}/invoice` -> `ApiResponse<InvoiceResponse>`
- **PDF Download:** `GET /api/orders/{id}/invoice/download` -> `byte[]` (`application/pdf`)
- **PDF Browser Preview:** `GET /api/orders/{id}/invoice/pdf` -> `byte[]` (`application/pdf`)

#### Order Cancellation
- **Method & Path:** `POST /api/orders/{id}/cancel` or `PATCH /api/orders/{id}/cancel`
- **Request Body (optional):** `Map<String, String>` (`location`, `description`)
- **Response Type:** `ApiResponse<OrderResponse>`
- **Cancellation Constraints:**
  - Orders with status `DELIVERED` **cannot** be cancelled directly. Backend throws `IllegalStateException` (`400 Bad Request`): `"Delivered orders cannot be cancelled directly. Please raise a return/dispute."`
  - Restores physical stock or releases consignment reservations.
  - Reverses any pending seller payout ledgers.

#### Order Disputes / Returns
- **Method & Path:** `POST /api/orders/{id}/return` or `POST /api/orders/{id}/dispute`
- **Request Body:** `OrderDisputeRequest` (`reason`, `description`, `images`)
- **Response Type:** `ApiResponse<OrderDisputeResponse>`

---

### 2.5 Other Customer Endpoints

#### Addresses
- `GET /api/addresses` or `GET /api/customer/addresses` -> `ApiResponse<List<AddressResponse>>`
- `POST /api/addresses` -> Body: `AddressRequest` -> `ApiResponse<AddressResponse>`
- `PUT /api/addresses/{id}` -> Body: `AddressRequest` -> `ApiResponse<AddressResponse>`
- `DELETE /api/addresses/{id}` -> `ApiResponse<Void>`
- `PATCH /api/addresses/{id}/default` -> `ApiResponse<AddressResponse>`

#### Reviews
- `GET /api/reviews/product/{productId}` -> `ApiResponse<List<ReviewResponse>>`
- `POST /api/reviews` -> Body: `ReviewRequest` (`productId`, `rating`, `comment`, `images`) -> `ApiResponse<ReviewResponse>`
- `PUT /api/reviews/{id}` -> Body: `UpdateReviewRequest` -> `ApiResponse<ReviewResponse>`
- `DELETE /api/reviews/{id}` -> `ApiResponse<Void>`

#### Notifications
- `(unverified / not present in backend)`: In-app notification inbox controller (`GET /api/notifications`) does not exist in backend controllers. System sends transactional emails via `ResendEmailServiceImpl` and Firebase event alerts.

#### Stores, Categories, and Brands
- `GET /api/stores` -> `ApiResponse<List<StoreResponse>>`
- `GET /api/stores/{slugOrId}` -> `ApiResponse<StoreResponse>`
- `GET /api/categories` -> `ApiResponse<List<CategoryResponse>>`
- `GET /api/categories/{id}` -> `ApiResponse<CategoryResponse>`
- `GET /api/categories/{id}/subcategories` -> `ApiResponse<List<SubcategoryResponse>>`
- `GET /api/brands` -> `ApiResponse<List<BrandResponse>>`
- `GET /api/brands/{id}` -> `ApiResponse<BrandResponse>`

---

## 3. SELLER

### 3.1 Seller Products (CRUD, Stock, Pricing)

- **Controller:** `com.example.project.customer.controller.SellerProductController`
- **Base Path:** `/api/seller/products`
- **Required Role:** `SELLER` or `ADMIN`

#### List Seller's Products
- **Method & Path:** `GET /api/seller/products`
- **Query Parameters:** `search`, `category`, `brand`, `status` (`PENDING`, `APPROVED`, `REJECTED`), `stockStatus`, `sortBy` (default `"newest"`), `page` (default 1), `limit` (default 12).
- **Response Type:** `SellerProductPageResponse`

#### Get Seller Product Details
- **Method & Path:** `GET /api/seller/products/{id}`
- **Note on ID Format:** The controller supports both numeric IDs (`123`) and prefixed IDs (`sp_123`).
- **Response Type:** `ApiResponse<ProductResponse>`

#### Create Seller Product
- **Method & Path:** `POST /api/seller/products`
- **Request Body DTO (`SellerProductCreateRequest`):**
  - `title` (`String`, required)
  - `categoryId` (`Integer`, required)
  - `subcategoryId` (`Integer`, optional)
  - `brandId` (`Integer`, optional)
  - `brand` (`String`, optional)
  - `sku` (`String`, optional)
  - `description` (`String`, optional)
  - `price` (`BigDecimal`, required)
  - `mrp` (`BigDecimal`, optional)
  - `stockQty` (`Integer`, required)
  - `unit` (`String`, optional)
  - `moq` (`Integer`, optional)
  - `images` (`List<String>`, optional)
  - `specifications` (`Map<String, String>`, optional)
- **Response Body:**
  ```json
  {
    "success": true,
    "message": "Product submitted successfully.",
    "data": {
      "id": "sp_123",
      "productId": "sp_123",
      "sellerId": "seller_5",
      "title": "TMT Bar 12mm",
      "sku": "SKU-TMT-12",
      "status": "PENDING",
      "createdAt": "2026-10-15T14:00:00"
    }
  }
  ```

#### Update Seller Product
- **Method & Path:** `PUT /api/seller/products/{id}`
- **Request Body DTO:** `SellerProductUpdateRequest`

#### Update Stock Quantity
- **Method & Path:** `PATCH /api/seller/products/{id}/stock`
- **Request Body DTO (`SellerStockUpdateRequest`):**
  - `stockQty` (`Integer`, required, min 0)
- **Response:** `{"success": true, "message": "Stock updated successfully", "data": {"id": "sp_123", "stockQty": 500}}`

#### Update Pricing
- **Method & Path:** `PATCH /api/seller/products/{id}/pricing`
- **Request Body DTO (`SellerPricingUpdateRequest`):**
  - `price` (`BigDecimal`, optional)
  - `mrp` (`BigDecimal`, optional)
  - `bulkPricingTiers` (`List<BulkPricingTier>`, optional)

#### Delete Product
- **Method & Path:** `DELETE /api/seller/products/{id}`

---

### 3.2 Seller Orders

- **Controller:** `com.example.project.customer.controller.SellerOrderController`
- **Base Path:** `/api/seller/orders`
- **Required Role:** `SELLER` or `ADMIN`

#### List Seller Orders
- **Method & Path:** `GET /api/seller/orders`
- **Query Parameters:** `status` (`String`, optional), `page` (`int`, default 1), `limit` (`int`, default 20)
- **Response Body:** `ApiResponse<SellerOrderPageData>`

#### Can Sellers See HinchStore Consignment Orders?
**YES.** Verified in `OrderRepository.findSellerOrders`:
```sql
WHERE (o.fulfillmentStore.seller.sellerId = :sellerId OR 
       o.store.seller.sellerId = :sellerId OR 
       oi.seller.sellerId = :sellerId OR 
       p.seller.sellerId = :sellerId)
```
When an order placed at HinchStore Central (`storeId = 1`) is routed to a vendor's fulfillment store (`o.fulfillmentStore.seller.sellerId = :sellerId`), the vendor seller **can** see and process the order in their seller portal.

#### Get Single Seller Order
- **Method & Path:** `GET /api/seller/orders/{id}`
- **Response Body:** `ApiResponse<SellerOrderDto>`

#### Seller Order Status Transitions
- **Method & Path:** `PATCH /api/seller/orders/{id}/status` or `PUT /api/seller/orders/{id}/status`
- **Request Body:**
  ```json
  {
    "status": "DISPATCHED",
    "location": "Warehouse Mumbai Hub",
    "notes": "Dispatched via BlueDart",
    "carrierName": "BlueDart",
    "vehicleNumber": "MH04AB1234",
    "driverName": "Ramesh Kumar",
    "trackingNumber": "BD8899123",
    "estimatedDelivery": "2026-10-18T18:00:00"
  }
  ```
- **Allowed Transitions for Sellers:**
  `PLACED` / `CONFIRMED` -> `PROCESSING` -> `DISPATCHED` -> `IN_TRANSIT`
- **RESTRICTION:** A seller **cannot** mark an order as `DELIVERED`. Attempting to pass `status: "DELIVERED"` results in `403 Forbidden`: `"Access denied: Only administrators can mark an order as DELIVERED."`

---

### 3.3 Seller Store Profile & Settings

- **Controller:** `StoreController`
- **Get Store Profile:** `GET /api/seller/store` -> `ApiResponse<StoreResponse>`
- **Update Store Profile:** `PUT /api/seller/store` -> Body: `StoreUpdateRequest` (`name`, `description`, `phone`, `email`, `address`, `city`, `state`, `pincode`, `bannerUrl`, `logoUrl`) -> `ApiResponse<StoreResponse>`

---

## 4. ADMIN

### 4.1 Seller Approval & Rejection

- **Controller:** `com.example.project.customer.controller.AdminSellerController`
- **Base Path:** `/api/admin/sellers`
- **Required Role:** `ADMIN`

#### List Sellers
- `GET /api/admin/sellers?status={PENDING|VERIFIED|REJECTED}&search={query}` -> `ApiResponse<List<Seller>>`
- `GET /api/admin/sellers/pending` -> `ApiResponse<List<Seller>>`
- `GET /api/admin/sellers/{sellerId}/summary` -> `ApiResponse<SellerOnboardingSummaryResponse>`
- `GET /api/admin/sellers/{sellerId}/vault` -> `ApiResponse<SellerDocumentVaultResponse>`

#### Approve Seller
- **Method & Path:** `POST /api/admin/sellers/{sellerId}/approve` (or `POST /api/admin/sellers/{sellerId}/verify`)
- **Request Parameters / Body:** `remarks` (`String`, optional) or `{"remarks": "Approved after KYC"}`
- **Response:** `ApiResponse<Seller>` with updated `verificationStatus: VERIFIED`.

#### Reject Seller
- **Method & Path:** `POST /api/admin/sellers/{sellerId}/reject`
- **Request Parameters / Body:** `remarks` (`String`, optional) or `{"remarks": "GST certificate blurred"}`
- **Response:** `ApiResponse<Seller>` with `verificationStatus: REJECTED`.

---

### 4.2 Admin Order Management & Dispute Resolution

- **Controller:** `com.example.project.customer.controller.AdminOrderController`
- **Base Path:** `/api/admin/orders`
- **Required Role:** `ADMIN`

#### List All Platform Orders
- **Method & Path:** `GET /api/admin/orders?status={status}&page={page}&limit={limit}`
- **Response Type:** `ApiResponse<List<OrderSummaryResponse>>`

#### Order Status Transition: Who Can Mark DELIVERED?
- **Endpoint:** `PUT /api/admin/orders/{id}/status` or `PATCH /api/admin/orders/{id}/status`
- **Rule:** **ONLY ADMINISTRATORS** can mark an order as `DELIVERED`.
- **Validation Rules in `OrderServiceImpl.updateOrderStatus`:**
  1. If caller does not possess `ROLE_ADMIN`, throws `ForbiddenException` (`403 Forbidden`).
  2. Prior status **must** be `DISPATCHED` or `IN_TRANSIT`. Otherwise, throws `IllegalArgumentException` (`400 Bad Request`): `"Order #<id> cannot be marked DELIVERED from status '<current>'. Allowed prior statuses are DISPATCHED or IN_TRANSIT."`
  3. If order is already `DELIVERED`, request is idempotent and returns the existing order.
  4. Marking `DELIVERED` automatically starts the **T+7 Dispute Settlement Period**: sets `settlementDate = LocalDateTime.now().plusDays(7)` on all associated `SellerPayoutLedger` records.

#### Cancel Order as Admin
- **Method & Path:** `POST /api/admin/orders/{id}/cancel`
- **Body:** `{"location": "Admin Panel", "description": "Cancelled by compliance officer"}`

---

### 4.3 Consignment / HinchStore Inventory Management

- **Controller:** `com.example.project.customer.controller.AdminConsignmentInventoryController`
- **Base Path:** `/api/admin/hinch-store/inventory`
- **Required Role:** `ADMIN`

#### Map or Update Consignment Inventory
- **Method & Path:** `POST /api/admin/hinch-store/inventory`
- **Request Body DTO (`HinchStoreInventoryRequest`):**
  - `vendorStoreId` (`Integer`, required): ID of vendor store fulfilling stock.
  - `productId` (`Integer`, required): Platform product ID in HinchStore Central.
  - `allocatedStockQty` (`Integer`, required, min 0): Total quantity allocated by vendor.
  - `reservedStockQty` (`Integer`, optional, min 0): Already reserved quantity.
  - `vendorPrice` (`BigDecimal`, optional): Vendor settlement price.
  - `isActive` (`Boolean`, optional): Default `true`.
- **Response Body (`ApiResponse<HinchStoreInventoryResponse>`):**
  - Real DTO: `com.example.project.customer.dto.consignment.HinchStoreInventoryResponse`
  - Fields: `id`, `vendorStoreId`, `vendorStoreName`, `productId`, `productTitle`, `allocatedStockQty`, `reservedStockQty`, `availableStockQty`, `vendorPrice`, `isActive`, `createdAt`, `updatedAt`.
  - HTTP Status: `201 Created`.

#### Toggle Consignment Inventory Status
- **Method & Path:** `PATCH /api/admin/hinch-store/inventory/{id}/status`
- **Request Body:** `{"isActive": true}` or `{"isActive": false}`
- **Response:** `ApiResponse<HinchStoreInventoryResponse>`

#### List Consignment Inventory Mappings
- **Method & Path:** `GET /api/admin/hinch-store/inventory?page={page}&limit={limit}`
- **Response Body:** `ApiResponse<List<HinchStoreInventoryResponse>>` with pagination metadata.

---

### 4.4 System Settings & Checkout Toggle

- **Config Class:** `com.example.project.customer.config.PlatformMarketplaceProperties`
- **Property:** `app.marketplace.hinch-store-checkout-enabled` (boolean, default: `false`)
- **Platform Store ID:** `app.marketplace.platform-store-id` (Integer, default: `1`)
- **Runtime Exposure:** Controlled via deployment environment variables (`APP_MARKETPLACE_HINCH_STORE_CHECKOUT_ENABLED=true/false`).
- **Behavior:** When `false`, order placement for store ID 1 is rejected with `HTTP 400 Bad Request` (`"HinchStore checkout is temporarily disabled pending Phase 2 vendor consignment rollout."`).

---

## 5. Known Gotchas & Frontend Traps

| Gotcha / Trap | Expected Trap | Actual Verified Backend Behavior | Recommendation for Frontend Developers |
| :--- | :--- | :--- | :--- |
| **Preview Endpoint Path** | Calling `POST /api/cart/preview` | Returns `404 Not Found`. Endpoint is `POST /api/checkout/preview`. | Update checkout API client to use `/api/checkout/preview`. |
| **Wishlist Availability Fields** | Expecting `inStock` / `availableQty` on `GET /api/wishlist` | Returns `availableQty: null`, `inStock: null`, `availabilityReason: null`. Only raw `stockQty` is mapped. | Do not use `item.product.inStock` from the wishlist response. Query `GET /api/products/{id}` for live badges or rely on Cart validation. |
| **Hot Deals Availability** | Slow loading or N+1 queries on homepage hot deals | `HomeHotDealServiceImpl` uses batch availability resolution (`getBatchAvailability`), populating all stock fields in a single pass. | Safely display stock counters and `"Hurry, only X left"` badges directly from `HotDealResponse.product.availableQty`. |
| **Order Cancellation & Gateway Refund Failure** | Expecting automatic fallback to wallet when Razorpay refund fails | **Automatic wallet fallback was removed in Phase 3.** If gateway refund fails, status becomes `REFUND_PENDING` for admin manual retry. Only non-Razorpay/offline orders credit the wallet. | Display: `"Refund Initiated via Original Payment Method. In case of delay, reference ID is queued for operational review."` Do not promise instant wallet credit. |
| **Consignment Store Visibility** | Expecting customer to see vendor store in catalog or cart | `ProductResponse` and `CartResponse` expose only `storeId: 1` (`"HinchStore Central"`). | Show `"Sold by HinchStore Central"` to buyers. Only `OrderResponse` exposes `fulfillmentStoreId` / `fulfillmentStoreName` post-order. |
| **DELIVERED Status Transition** | Seller trying to mark order as `DELIVERED` from seller app | Throws `ForbiddenException` -> `HTTP 403 Forbidden`. Only `ADMIN` can mark `DELIVERED`. | Disable or hide the `"Mark as Delivered"` button in the Seller Portal. Only allow sellers to advance orders up to `DISPATCHED` / `IN_TRANSIT`. |
| **Consignment Inventory Editing Endpoint** | Believing vendor inventory can only be seeded via direct DB scripts | Admin controller exists: `POST /api/admin/hinch-store/inventory` and `PATCH /api/admin/hinch-store/inventory/{id}/status`. | Build admin consignment management screen calling `/api/admin/hinch-store/inventory`. |
| **Stock Shortage HTTP Status** | Expecting HTTP 400 for out of stock | `CartServiceImpl` throws `ResourceConflictException`, which `GlobalExceptionHandler` maps to **`HTTP 409 Conflict`**. | In frontend cart error handlers, intercept `409 Conflict` alongside `400` to display stock warning banners. |
| **Store Mismatch Error Schema** | Expecting standard `ApiResponse` envelope for multi-store cart error | `StoreMismatchException` returns custom JSON: `{status: 409, error: "STORE_MISMATCH", currentStore: {...}, newStore: {...}}`. | Check `error === "STORE_MISMATCH"` on 409 to trigger the Store Switch confirmation modal. |
