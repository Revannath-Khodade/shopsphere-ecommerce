# ShopSphere API Documentation

Base URL (local development): `http://localhost:8080/api`

Interactive documentation (Swagger UI): `http://localhost:8080/api/swagger-ui.html`
Raw OpenAPI spec: `http://localhost:8080/api/v3/api-docs`

All endpoints below are relative to the base URL and are versioned under `/v1`.

## Conventions

**Response envelope.** Every successful response is wrapped in `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Request successful",
  "data": { },
  "timestamp": "2026-07-16T10:15:30"
}
```

**Error envelope.** Every error response uses the same shape, produced by the global exception handler:
```json
{
  "timestamp": "2026-07-16T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: '42'",
  "path": "/api/v1/products/42"
}
```
Validation failures additionally include a `validationErrors` map of field name → message.

**Authentication.** Protected endpoints require an `Authorization: Bearer <accessToken>` header. Obtain a token via `POST /v1/auth/register` or `POST /v1/auth/login`. Access tokens expire after 15 minutes (`app.jwt.expiration-ms`); use `POST /v1/auth/refresh` to obtain a new one without re-entering credentials.

**Pagination.** Paginated endpoints accept `page` (0-indexed, default `0`), `size` (default `10`, max `100`), `sortBy` (default `id`), and `sortDirection` (`asc`/`desc`, default `asc`) query parameters, and return a `PaginationResponse<T>`:
```json
{
  "content": [ ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

**Roles.** `ROLE_CUSTOMER` (default on registration), `ROLE_SELLER`, `ROLE_ADMIN`.

---

## 1. Authentication APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/auth/register` | Public | Create a new customer account |
| POST | `/v1/auth/login` | Public | Log in with username/email + password |
| POST | `/v1/auth/refresh` | Public | Exchange a refresh token for a new access token |
| POST | `/v1/auth/logout` | Public | Revoke a refresh token |

### POST `/v1/auth/register`
**Request:**
```json
{
  "username": "amit_verma",
  "email": "amit@example.com",
  "password": "Password123!",
  "firstName": "Amit",
  "lastName": "Verma",
  "phone": "+91-9000000000"
}
```
**Response `201 Created`:**
```json
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { "id": 10, "username": "amit_verma", "email": "amit@example.com", "roles": ["ROLE_CUSTOMER"] }
  }
}
```
Errors: `400` validation failure, `409` username/email already exists.

### POST `/v1/auth/login`
**Request:** `{ "usernameOrEmail": "amit_verma", "password": "Password123!" }`
**Response `200 OK`:** same shape as register.
Errors: `401` invalid credentials.

### POST `/v1/auth/refresh`
**Request:** `{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }`
**Response `200 OK`:** new `token` + rotated `refreshToken`.
Errors: `401` refresh token invalid, expired, or revoked.

### POST `/v1/auth/logout`
**Request:** `{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }`
**Response `200 OK`:** `{ "success": true, "message": "Logged out successfully" }`

---

## 2. User APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/v1/users/profile` | Any authenticated user | Get own profile |
| PUT | `/v1/users/profile` | Any authenticated user | Update own profile |
| DELETE | `/v1/users/profile` | Any authenticated user | Delete own account |
| GET | `/v1/users/{id}` | ROLE_ADMIN | Get any user by id |
| GET | `/v1/users` | ROLE_ADMIN | List every user |
| DELETE | `/v1/users/{id}` | ROLE_ADMIN | Delete any user by id |

**UserRequest (PUT /profile):**
```json
{ "email": "amit.verma@example.com", "firstName": "Amit", "lastName": "Verma", "phone": "+91-9000000001" }
```

---

## 3. Category APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/categories` | ROLE_ADMIN | Create a category |
| PUT | `/v1/categories/{id}` | ROLE_ADMIN | Update a category |
| DELETE | `/v1/categories/{id}` | ROLE_ADMIN | Delete a category (must have no products) |
| GET | `/v1/categories/{id}` | Public | Get a category by id |
| GET | `/v1/categories` | Public | List every category |
| GET | `/v1/categories/active` | Public | List only active categories |
| GET | `/v1/categories/top-level` | Public | List only top-level (no parent) categories |

**CategoryRequest:**
```json
{ "name": "Electronics", "description": "Gadgets and devices", "parentCategoryId": null, "active": true }
```

---

## 4. Product APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/products` | ROLE_SELLER / ROLE_ADMIN | Create a product |
| PUT | `/v1/products/{id}` | Owning seller / ROLE_ADMIN | Update a product |
| DELETE | `/v1/products/{id}` | Owning seller / ROLE_ADMIN | Delete a product |
| GET | `/v1/products/{id}` | Public | Get a product by id |
| GET | `/v1/products` | Public | Paginated list of active products |
| GET | `/v1/products/search?keyword=` | Public | Search by name/description |
| GET | `/v1/products/filter?categoryId=&brand=&minPrice=&maxPrice=` | Public | Multi-criteria filter (all params optional) |
| GET | `/v1/products/category/{categoryId}` | Public | Products within a category |
| GET | `/v1/products/seller/{sellerId}` | Public | Products listed by a seller |
| GET | `/v1/products/latest` | Public | 10 most recently added products |

**ProductRequest:**
```json
{
  "name": "Wireless Bluetooth Headphones",
  "description": "Over-ear headphones with ANC.",
  "sku": "SKU-ELEC-0001",
  "price": 2999.00,
  "discountPrice": 2499.00,
  "stockQuantity": 150,
  "brand": "SoundWave",
  "active": true,
  "categoryId": 1,
  "imageUrls": ["https://cdn.shopsphere.com/products/SKU-ELEC-0001/main.jpg"]
}
```
Errors: `400` validation / discount > price, `403` not the owning seller, `404` category not found, `409` duplicate SKU.

---

## 5. Cart APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/v1/cart` | Any authenticated user | View own cart |
| POST | `/v1/cart/items` | Any authenticated user | Add an item (merges if already present) |
| PUT | `/v1/cart/items/{productId}?quantity=` | Any authenticated user | Update a line item's quantity |
| DELETE | `/v1/cart/items/{productId}` | Any authenticated user | Remove a line item |
| DELETE | `/v1/cart` | Any authenticated user | Clear the entire cart |

**CartRequest:** `{ "productId": 1, "quantity": 2 }`
Errors: `404` product not found, `409` insufficient stock.

---

## 6. Order APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/orders` | Any authenticated user | Place an order from the current cart |
| PUT | `/v1/orders/{id}/cancel` | Order owner | Cancel an order (restocks items) |
| GET | `/v1/orders` | Any authenticated user | Own paginated order history |
| GET | `/v1/orders/{id}` | Order owner | Order details by id |
| GET | `/v1/orders/track/{orderNumber}` | Any authenticated user | Look up by order number |
| GET | `/v1/orders/admin/all?status=` | ROLE_ADMIN | Every order, optionally filtered by status |

**OrderRequest:**
```json
{ "shippingAddressId": 1, "billingAddressId": 1, "paymentMethod": "CASH_ON_DELIVERY", "notes": "Deliver in the evening." }
```
Errors: `400` cart empty, `403` address doesn't belong to caller, `409` insufficient stock at checkout.

---

## 7. Payment APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/payments` | Any authenticated user | Record a payment for an order |
| GET | `/v1/payments/order/{orderId}` | Any authenticated user | Full payment details |
| GET | `/v1/payments/order/{orderId}/status` | Any authenticated user | Payment status only (polling) |
| PUT | `/v1/payments/{id}/status?status=` | ROLE_ADMIN | Update payment status (also transitions order status) |

**PaymentRequest:** `{ "orderId": 1, "paymentMethod": "CASH_ON_DELIVERY", "amount": 2999.00, "transactionId": null }`
Status values: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`, `CANCELLED`.

---

## 8. Review APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/reviews` | Any authenticated user | Add a review (requires verified purchase) |
| PUT | `/v1/reviews/{id}` | Review owner | Update own review |
| DELETE | `/v1/reviews/{id}` | Review owner | Delete own review |
| GET | `/v1/reviews/product/{productId}` | Public | Paginated reviews for a product |
| GET | `/v1/reviews/product/{productId}/average-rating` | Public | Average rating (0.0 if none) |

**ReviewRequest:** `{ "productId": 1, "rating": 5, "comment": "Excellent product!" }`
Errors: `400` not a verified purchaser, `409` already reviewed this product.

---

## 9. Wishlist APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/wishlist/{productId}` | Any authenticated user | Add a product to the wishlist |
| DELETE | `/v1/wishlist/{productId}` | Any authenticated user | Remove a product from the wishlist |
| GET | `/v1/wishlist` | Any authenticated user | View own wishlist |

Errors: `404` not in wishlist (on remove), `409` already in wishlist (on add).

---

## 10. Address APIs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/v1/addresses` | Any authenticated user | Add an address (first one becomes default automatically) |
| PUT | `/v1/addresses/{id}` | Address owner | Update an address |
| DELETE | `/v1/addresses/{id}` | Address owner | Delete an address |
| GET | `/v1/addresses` | Any authenticated user | List own addresses |
| GET | `/v1/addresses/default` | Any authenticated user | Get own default address |
| PUT | `/v1/addresses/{id}/default` | Address owner | Set an address as default |

**AddressRequest:**
```json
{
  "addressLine1": "221B Baker Street",
  "addressLine2": "Near Central Park",
  "city": "Mumbai",
  "state": "Maharashtra",
  "postalCode": "400001",
  "country": "India",
  "addressType": "BOTH",
  "isDefault": true
}
```

---

## HTTP Status Code Reference

| Code | Meaning | When |
|---|---|---|
| 200 | OK | Successful GET/PUT/DELETE |
| 201 | Created | Successful POST that creates a resource |
| 400 | Bad Request | Validation failure, business rule violation (e.g. empty cart, mismatched discount) |
| 401 | Unauthorized | Missing/invalid/expired token, bad login credentials |
| 403 | Forbidden | Authenticated but lacking the required role, or not the resource owner |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (SKU/email/username), insufficient stock, already-reviewed |
| 500 | Internal Server Error | Unexpected server-side failure |

## Postman Collection

Import `postman/ShopSphere.postman_collection.json` and `postman/ShopSphere.postman_environment.json` into Postman. Run **Authentication → Login** first — it automatically stores `accessToken` and `refreshToken` as environment variables, which every other request in the collection reuses via collection-level Bearer authentication.
