# ShopSphere — Backend (Phase 1: Foundation)

ShopSphere is a production-grade full-stack e-commerce platform. This repository
contains **Phase 1** of the backend: the foundational layer only — project setup,
database design, JPA entities, and repositories.

> **Out of scope for Phase 1 (intentionally):** Controllers, Services, Spring Security,
> JWT authentication, Global Exception Handling, DTOs, and the React frontend. These
> arrive in later phases. Empty packages for them have already been scaffolded so the
> project structure won't need to be reshuffled later.

---

## 1. Project Overview

| | |
|---|---|
| **Project Name** | ShopSphere |
| **Module** | shopsphere-backend |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.4 |
| **Build Tool** | Maven |
| **Database** | MySQL 8.x |
| **ORM** | Spring Data JPA / Hibernate |
| **Auth (future)** | JWT (jjwt) |
| **API Docs (future)** | springdoc-openapi / Swagger UI |
| **Frontend (future)** | React + Vite |

---

## 2. Folder Structure

```
shopsphere-backend
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── shopsphere
│   │   │           ├── ShopsphereBackendApplication.java
│   │   │           ├── config/            (empty — Phase 2+)
│   │   │           ├── controller/        (empty — Phase 2+)
│   │   │           ├── dto/
│   │   │           │   ├── request/       (empty — Phase 2+)
│   │   │           │   └── response/      (empty — Phase 2+)
│   │   │           ├── entity/
│   │   │           │   ├── Address.java
│   │   │           │   ├── Cart.java
│   │   │           │   ├── CartItem.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Order.java
│   │   │           │   ├── OrderItem.java
│   │   │           │   ├── Payment.java
│   │   │           │   ├── Product.java
│   │   │           │   ├── ProductImage.java
│   │   │           │   ├── Review.java
│   │   │           │   ├── Role.java
│   │   │           │   ├── User.java
│   │   │           │   ├── Wishlist.java
│   │   │           │   └── enums/
│   │   │           │       ├── AddressType.java
│   │   │           │       ├── OrderStatus.java
│   │   │           │       ├── PaymentMethod.java
│   │   │           │       ├── PaymentStatus.java
│   │   │           │       └── RoleName.java
│   │   │           ├── exception/         (empty — Phase 2+)
│   │   │           ├── repository/
│   │   │           │   ├── AddressRepository.java
│   │   │           │   ├── CartItemRepository.java
│   │   │           │   ├── CartRepository.java
│   │   │           │   ├── CategoryRepository.java
│   │   │           │   ├── OrderItemRepository.java
│   │   │           │   ├── OrderRepository.java
│   │   │           │   ├── PaymentRepository.java
│   │   │           │   ├── ProductImageRepository.java
│   │   │           │   ├── ProductRepository.java
│   │   │           │   ├── ReviewRepository.java
│   │   │           │   ├── RoleRepository.java
│   │   │           │   ├── UserRepository.java
│   │   │           │   └── WishlistRepository.java
│   │   │           ├── security/          (empty — Phase 2+)
│   │   │           ├── service/           (empty — Phase 2+)
│   │   │           │   └── impl/          (empty — Phase 2+)
│   │   │           └── util/              (empty — Phase 2+)
│   │   └── resources
│   │       ├── application.properties
│   │       └── db
│   │           ├── schema.sql
│   │           └── insert.sql
│   └── test
│       └── java/com/shopsphere/    (empty — Phase 2+)
```

---

## 3. Database Design

All tables use `InnoDB` + `utf8mb4`. Money columns use `DECIMAL(12,2)` (never
float/double). Timestamps default to `CURRENT_TIMESTAMP`. Full DDL lives in
[`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql).

### 3.1 `roles`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(30) | NOT NULL, UNIQUE, CHECK IN (ROLE_ADMIN, ROLE_SELLER, ROLE_CUSTOMER) |
| description | VARCHAR(255) | |

### 3.2 `users`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(100) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (BCrypt hash) |
| first_name | VARCHAR(50) | NOT NULL |
| last_name | VARCHAR(50) | NOT NULL |
| phone | VARCHAR(20) | |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE |
| account_non_locked | BOOLEAN | NOT NULL, DEFAULT TRUE |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP |

### 3.3 `user_roles` (join table)
| Column | Type | Constraints |
|---|---|---|
| user_id | BIGINT | PK (composite), FK → users.id (ON DELETE CASCADE) |
| role_id | BIGINT | PK (composite), FK → roles.id (ON DELETE CASCADE) |

### 3.4 `categories`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | NOT NULL, UNIQUE |
| description | VARCHAR(500) | |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE |
| parent_id | BIGINT | FK → categories.id (self-referencing, ON DELETE SET NULL) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### 3.5 `products`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(150) | NOT NULL |
| description | TEXT | |
| sku | VARCHAR(50) | NOT NULL, UNIQUE |
| price | DECIMAL(12,2) | NOT NULL, CHECK (price >= 0) |
| discount_price | DECIMAL(12,2) | |
| stock_quantity | INT | NOT NULL, DEFAULT 0, CHECK (>= 0) |
| brand | VARCHAR(100) | |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE |
| category_id | BIGINT | NOT NULL, FK → categories.id (ON DELETE RESTRICT) |
| seller_id | BIGINT | NOT NULL, FK → users.id (ON DELETE RESTRICT) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP |

Indexes: `idx_products_category`, `idx_products_seller`, `idx_products_name`

### 3.6 `product_images`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| image_url | VARCHAR(500) | NOT NULL |
| alt_text | VARCHAR(150) | |
| is_primary | BOOLEAN | NOT NULL, DEFAULT FALSE |
| display_order | INT | NOT NULL, DEFAULT 0 |
| product_id | BIGINT | NOT NULL, FK → products.id (ON DELETE CASCADE) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### 3.7 `addresses`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| address_line1 | VARCHAR(255) | NOT NULL |
| address_line2 | VARCHAR(255) | |
| city | VARCHAR(100) | NOT NULL |
| state | VARCHAR(100) | NOT NULL |
| postal_code | VARCHAR(20) | NOT NULL |
| country | VARCHAR(100) | NOT NULL |
| address_type | VARCHAR(20) | NOT NULL, DEFAULT 'BOTH', CHECK IN (SHIPPING, BILLING, BOTH) |
| is_default | BOOLEAN | NOT NULL, DEFAULT FALSE |
| user_id | BIGINT | NOT NULL, FK → users.id (ON DELETE CASCADE) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### 3.8 `carts`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, UNIQUE, FK → users.id (ON DELETE CASCADE) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP |

### 3.9 `cart_items`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| quantity | INT | NOT NULL, DEFAULT 1, CHECK (> 0) |
| price | DECIMAL(12,2) | NOT NULL (unit price snapshot) |
| cart_id | BIGINT | NOT NULL, FK → carts.id (ON DELETE CASCADE) |
| product_id | BIGINT | NOT NULL, FK → products.id (ON DELETE CASCADE) |
| added_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

Constraint: `UNIQUE (cart_id, product_id)` — one row per product per cart.

### 3.10 `orders`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| order_number | VARCHAR(40) | NOT NULL, UNIQUE |
| total_amount | DECIMAL(12,2) | NOT NULL |
| shipping_fee | DECIMAL(12,2) | NOT NULL, DEFAULT 0.00 |
| tax_amount | DECIMAL(12,2) | NOT NULL, DEFAULT 0.00 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK IN (8 states) |
| order_date | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP |
| notes | VARCHAR(500) | |
| user_id | BIGINT | NOT NULL, FK → users.id (ON DELETE RESTRICT) |
| shipping_address_id | BIGINT | NOT NULL, FK → addresses.id (ON DELETE RESTRICT) |
| billing_address_id | BIGINT | NOT NULL, FK → addresses.id (ON DELETE RESTRICT) |

### 3.11 `order_items`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| quantity | INT | NOT NULL, CHECK (> 0) |
| price | DECIMAL(12,2) | NOT NULL (unit price snapshot at purchase time) |
| subtotal | DECIMAL(12,2) | NOT NULL |
| order_id | BIGINT | NOT NULL, FK → orders.id (ON DELETE CASCADE) |
| product_id | BIGINT | NOT NULL, FK → products.id (ON DELETE RESTRICT) |

### 3.12 `payments`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| payment_method | VARCHAR(20) | NOT NULL, CHECK IN (7 methods) |
| transaction_id | VARCHAR(100) | UNIQUE |
| amount | DECIMAL(12,2) | NOT NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK IN (5 states) |
| order_id | BIGINT | NOT NULL, UNIQUE, FK → orders.id (ON DELETE CASCADE) |
| payment_date | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### 3.13 `reviews`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| rating | INT | NOT NULL, CHECK BETWEEN 1 AND 5 |
| comment | TEXT | |
| user_id | BIGINT | NOT NULL, FK → users.id (ON DELETE CASCADE) |
| product_id | BIGINT | NOT NULL, FK → products.id (ON DELETE CASCADE) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

Constraint: `UNIQUE (user_id, product_id)` — one review per user per product.

### 3.14 `wishlist`
| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| user_id | BIGINT | NOT NULL, FK → users.id (ON DELETE CASCADE) |
| product_id | BIGINT | NOT NULL, FK → products.id (ON DELETE CASCADE) |
| created_at | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

Constraint: `UNIQUE (user_id, product_id)` — a product appears once per wishlist.

---

## 4. ER Diagram

```
Role  }o------o{  User  (via user_roles)

User
  |
  |----< Address           (1 user  -> many addresses)
  |----o Cart               (1 user  -> 1 cart)
  |----< Order              (1 user  -> many orders)
  |----< Review             (1 user  -> many reviews)
  |----< Wishlist           (1 user  -> many wishlist rows)
  |----< Product            (1 user[seller] -> many products listed)

Category
  |
  |----< Category            (self-referencing: parent -> subcategories)
  |----< Product             (1 category -> many products)

Product
  |
  |----< ProductImage        (1 product -> many images)
  |----< CartItem            (1 product -> many cart_items)
  |----< OrderItem           (1 product -> many order_items)
  |----< Review              (1 product -> many reviews)
  |----< Wishlist            (1 product -> many wishlist rows)

Cart
  |
  |----< CartItem            (1 cart -> many cart_items)

Order
  |
  |----< OrderItem           (1 order -> many order_items)
  |----o Payment             (1 order -> 1 payment)
  |----> Address (shipping)  (many orders -> 1 shipping address)
  |----> Address (billing)   (many orders -> 1 billing address)
```

**Legend:** `----<` = one-to-many, `----o` = one-to-one, `}o------o{` = many-to-many, `---->` = many-to-one

### Relationship Summary

| Relationship | Type | Notes |
|---|---|---|
| User ↔ Role | Many-to-Many | via `user_roles` join table |
| User → Address | One-to-Many | a user has multiple saved addresses |
| User → Cart | One-to-One | each user has exactly one cart |
| User → Order | One-to-Many | order history |
| User → Review | One-to-Many | reviews written by the user |
| User → Wishlist | One-to-Many | wishlist entries |
| User (seller) → Product | One-to-Many | products a seller has listed |
| Category → Category | One-to-Many (self) | optional parent/subcategory hierarchy |
| Category → Product | One-to-Many | product classification |
| Product → ProductImage | One-to-Many | gallery images |
| Product → CartItem | One-to-Many | which carts reference this product |
| Product → OrderItem | One-to-Many | which orders reference this product |
| Product → Review | One-to-Many | reviews for the product |
| Product → Wishlist | One-to-Many | users who wishlisted this product |
| Cart → CartItem | One-to-Many | items currently in the cart |
| Order → OrderItem | One-to-Many | purchased line items |
| Order → Payment | One-to-One | payment record for the order |
| Order → Address (shipping/billing) | Many-to-One (x2) | reuses the `addresses` table for both roles |

---

## 5. Entities (JPA)

All 13 entities live under `com.shopsphere.entity` and use Lombok
(`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`,
`@ToString` with relationship fields excluded to avoid recursive/lazy-loading
issues). Auditing timestamps use Hibernate's `@CreationTimestamp` /
`@UpdateTimestamp`. All relationships specify explicit `fetch` types
(`LAZY` by default, `EAGER` only for `User.roles` since roles are small and
needed on nearly every authenticated request) and named foreign keys via
`@ForeignKey` for clean, readable generated DDL.

| Entity | Table | Key Relationships |
|---|---|---|
| `Role` | roles | `@ManyToMany(mappedBy)` → User |
| `User` | users | `@ManyToMany` → Role, `@OneToMany` → Address/Order/Review/Wishlist/Product, `@OneToOne` → Cart |
| `Category` | categories | self `@ManyToOne`/`@OneToMany`, `@OneToMany` → Product |
| `Product` | products | `@ManyToOne` → Category/User(seller), `@OneToMany` → ProductImage/CartItem/OrderItem/Review/Wishlist |
| `ProductImage` | product_images | `@ManyToOne` → Product |
| `Cart` | carts | `@OneToOne` → User, `@OneToMany` → CartItem |
| `CartItem` | cart_items | `@ManyToOne` → Cart, Product |
| `Address` | addresses | `@ManyToOne` → User |
| `Order` | orders | `@ManyToOne` → User/Address (x2), `@OneToMany` → OrderItem, `@OneToOne` → Payment |
| `OrderItem` | order_items | `@ManyToOne` → Order, Product |
| `Payment` | payments | `@OneToOne` → Order |
| `Review` | reviews | `@ManyToOne` → User, Product |
| `Wishlist` | wishlist | `@ManyToOne` → User, Product |

---

## 6. Repository Layer

All repositories extend `JpaRepository<Entity, Long>` and include targeted
derived-query and `@Query` methods, e.g.:

- `UserRepository.findByEmail`, `existsByUsername`, `findByUsernameOrEmail`
- `ProductRepository.findByCategoryId`, `searchByKeyword`, `findTop10ByOrderByCreatedAtDesc`, `findByStockQuantityLessThan`
- `OrderRepository.findByOrderNumber`, `findByUserId` (paged), `findTop10ByUserIdOrderByOrderDateDesc`
- `ReviewRepository.findAverageRatingByProductId` (custom `@Query`)
- `WishlistRepository.existsByUserIdAndProductId`

See the `repository/` package for the full list.

---

## 7. Prerequisites

- **JDK 21**
- **Maven 3.9+**
- **MySQL 8.x** running locally (or reachable) — create no database manually;
  `schema.sql` creates `shopsphere_db` for you
- An IDE (IntelliJ IDEA recommended) with Lombok plugin enabled

---

## 8. How to Run (Phase 1)

Phase 1 has no controllers, so there are no REST endpoints to hit yet — this
step verifies the project compiles, the entities map correctly, and the
schema/seed scripts are valid.

1. **Start MySQL** and confirm you can connect with the credentials in
   `src/main/resources/application.properties` (defaults: `root` / `root`).

2. **Apply the schema and seed data manually** (Phase 1 does not auto-run SQL
   on boot — `spring.sql.init.mode=never` by design, since `ddl-auto=validate`
   expects the schema to already exist):
   ```bash
   mysql -u root -p < src/main/resources/db/schema.sql
   mysql -u root -p < src/main/resources/db/insert.sql
   ```

3. **Update credentials** in `application.properties` if your local MySQL
   username/password differ from `root`/`root`.

4. **Build the project:**
   ```bash
   mvn clean install
   ```

5. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```
   The app boots on `http://localhost:8080/api`. With `ddl-auto=validate`,
   Hibernate will validate that every entity mapping matches the schema you
   applied in step 2 — a good end-to-end sanity check for Phase 1.

---

## 9. Checklist — Phase 1 Deliverables

- [x] Complete Maven project folder structure (industry-standard package layout)
- [x] Normalized MySQL database design for all 14 tables (13 requested + `user_roles` join table)
- [x] ER Diagram (Markdown)
- [x] `schema.sql` — full DDL: tables, PKs, FKs, constraints, indexes
- [x] `insert.sql` — seed data: 1 Admin, 1 Seller, 1 Customer, 5 Categories, 10 Products (+ images, address, cart)
- [x] 13 JPA Entity classes (Lombok, relationships, cascade/fetch types, builder pattern)
- [x] 5 supporting enums (`RoleName`, `OrderStatus`, `PaymentStatus`, `PaymentMethod`, `AddressType`)
- [x] 13 Repository interfaces (JpaRepository + custom query methods)
- [x] `application.properties` (MySQL, JPA/Hibernate, logging, server port, JWT placeholders)
- [x] `pom.xml` with all required dependencies (Web, JPA, Security, Validation, MySQL, JWT, Lombok, OpenAPI, DevTools, Testing)
- [x] README (this file)
- [ ] Controllers — **Phase 2**
- [ ] Services / Service impls — **Phase 2**
- [ ] Spring Security + JWT — **Phase 2**
- [ ] Global Exception Handling — **Phase 2**
- [ ] DTOs — **Phase 2**
- [ ] React + Vite frontend — later phase

---

## 10. Phase 2 — Business Layer

Phase 2 adds the complete business layer on top of the Phase 1 foundation:
DTOs, ModelMapper-backed mappers, service interfaces/implementations,
centralized exception handling, reusable response wrappers, utilities, and a
JUnit 5 + Mockito unit test foundation. Controllers, Spring Security, JWT,
and the React frontend remain out of scope (later phases).

### 10.1 New/changed dependencies
- **pom.xml**: added `org.modelmapper:modelmapper:3.2.1` (the only Phase 1 file touched — an additive dependency, required for the mapper layer).

### 10.2 New packages
```
com.shopsphere
├── config
│   ├── MapperConfig.java            (ModelMapper bean)
│   └── PasswordEncoderConfig.java   (BCryptPasswordEncoder bean — NOT full Spring Security)
├── dto
│   ├── ApiResponse.java
│   ├── PaginationResponse.java
│   ├── request/    (10 request DTOs)
│   └── response/   (13 response DTOs)
├── exception
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java  (@RestControllerAdvice)
│   └── 9 custom exception classes
├── mapper           (8 mapper components)
├── service           (10 interfaces)
├── service.impl       (10 implementations)
└── util
    ├── AppConstants.java
    ├── DateUtil.java
    ├── OrderNumberGenerator.java
    ├── PaginationUtil.java
    └── ValidationUtil.java
```

### 10.3 Design notes

- **DTOs** use Bean Validation (`@NotBlank`, `@NotNull`, `@Email`, `@Pattern`, `@Positive`/`@PositiveOrZero`, `@Size`, `@Min`/`@Max`, `@DecimalMin`, `@Digits`) so a future `@Valid @RequestBody` controller layer gets request validation "for free."
- **Mappers** are hand-written `@Component` classes (not blind ModelMapper reflection) for anything with nested collections or derived fields (e.g. `Product.averageRating`, `Cart.totalAmount`) — `ModelMapper` itself is configured and available via `MapperConfig`, but structural entity graphs are mapped explicitly for correctness and readability.
- **`AuthenticationServiceImpl`** hashes passwords with `PasswordEncoder` (BCrypt) and validates login credentials, but issues an **opaque placeholder token** (not a JWT) — real JWT issuance is deliberately deferred to the Security phase, per this phase's scope. The `AuthenticationResponse` contract will not need to change when that happens.
- **`OrderServiceImpl.placeOrder`** re-validates stock at checkout (not just at add-to-cart time), decrements inventory, creates a `PENDING` `Payment` row, and only clears the cart after the order is persisted.
- **`ProductServiceImpl`** enriches every `ProductResponse` with a live average rating and review count computed via `ReviewRepository`, without polluting the pure structural `ProductMapper`.
- **`ReviewServiceImpl.addReview`** enforces a verified-purchase rule: a customer can only review a product they have an `OrderItem` for.
- All service methods use **constructor injection** (via Lombok `@RequiredArgsConstructor`), explicit `@Transactional` / `@Transactional(readOnly = true)`, and SLF4J logging (`@Slf4j`) around create/update/delete/auth/order/payment operations and all thrown business exceptions.
- **`GlobalExceptionHandler`** maps every custom exception (plus `MethodArgumentNotValidException`, `DataIntegrityViolationException`, `AccessDeniedException`, `BadCredentialsException`) to the shared `ErrorResponse` JSON shape with the correct HTTP status.

### 10.4 Checklist — Phase 2 Deliverables

- [x] 10 Request DTOs + 13 Response DTOs (including nested `CartItemResponse`, `OrderItemResponse`, `ProductImageResponse`) with Bean Validation annotations
- [x] `ApiResponse<T>` and `PaginationResponse<T>` reusable wrappers
- [x] `MapperConfig` (ModelMapper bean) + 8 mapper components (User, Category, Product, Address, Cart, Order, Payment, Review, Wishlist)
- [x] 10 Service interfaces
- [x] 10 Service implementations with full business logic (register/login, profile CRUD, category CRUD, product CRUD + pagination/sorting/filtering/search/latest/by-category/by-seller, cart add/update/remove/clear + total calculation, order placement/cancellation/history/details with stock validation and cart-empty checks, payment save/status update, review add/update/delete/average-rating with verified-purchase + duplicate-review validation, wishlist add/remove/list, address CRUD with default-address handling)
- [x] Cross-field/business validations: duplicate email/username/SKU/category-name, product/category existence, stock sufficiency, cart-empty, quantity positivity, payment-amount matching, review eligibility
- [x] 9 custom exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `CartEmptyException`, `OutOfStockException`, `PaymentFailedException`, `OrderNotFoundException`)
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ErrorResponse` JSON shape
- [x] Util package: `AppConstants`, `DateUtil`, `ValidationUtil`, `PaginationUtil`, `OrderNumberGenerator`
- [x] SLF4J logging throughout (create/update/delete/auth/order/payment/errors)
- [x] JUnit 5 + Mockito unit test foundation: 5 test classes covering `AuthenticationServiceImpl`, `ProductServiceImpl`, `CartServiceImpl`, `OrderServiceImpl`, `ReviewServiceImpl`
- [ ] Controllers — **Phase 3**
- [ ] Spring Security + JWT — **Phase 3**
- [ ] React + Vite frontend — later phase

---

## 11. Phase 3 — Spring Security + JWT Authentication

Phase 3 adds the complete security layer: stateless JWT authentication,
refresh-token rotation with server-side revocation, role-based authorization,
and centralized security exception handling. Controllers, the React
frontend, and admin/seller dashboards remain out of scope (later phases).

### 11.1 Files created

**Entity / persistence**
- `entity/RefreshToken.java`, `repository/RefreshTokenRepository.java`
- `db/schema.sql` — added table `refresh_tokens` (+ `DROP TABLE` cleanup entry)

**Security core** (`com.shopsphere.security`)
- `CustomUserDetails.java` — wraps `User` as a Spring Security `UserDetails`, maps `Role` → `GrantedAuthority`
- `CustomUserDetailsService.java` — loads a user by username **or** email
- `JwtTokenProvider.java` — low-level JWT signing/parsing (jjwt), no domain knowledge
- `JwtService.java` — domain-level token issuance/validation (access + refresh, claims: `userId`, `roles`, `type`)
- `JwtAuthenticationFilter.java` — `OncePerRequestFilter`; extracts, validates, and authenticates every request
- `JwtAuthenticationEntryPoint.java` — 401 handler
- `JwtAccessDeniedHandler.java` — 403 handler

**Configuration**
- `config/SecurityConfig.java` — `SecurityFilterChain`, `PasswordEncoder`, `AuthenticationProvider`, `AuthenticationManager`, CORS, CSRF, stateless sessions, role-based `requestMatchers`, `@EnableMethodSecurity`

**Business layer**
- `service/RefreshTokenService.java` + `service/impl/RefreshTokenServiceImpl.java`
- `dto/request/RefreshTokenRequest.java`, `dto/request/LogoutRequest.java`

**Tests**
- `security/JwtTokenProviderTest.java`, `security/JwtServiceTest.java`, `security/CustomUserDetailsServiceTest.java`
- `service/impl/RefreshTokenServiceImplTest.java`

### 11.2 Files modified

| File | Change |
|---|---|
| `service/AuthenticationService.java` | Added `refreshToken()` and `logout()` to the contract |
| `service/impl/AuthenticationServiceImpl.java` | Login now delegates credential checks to Spring Security's `AuthenticationManager`; both register/login issue real signed JWTs + persisted refresh tokens instead of the Phase 2 placeholder token |
| `dto/response/AuthenticationResponse.java` | Added `refreshToken` and `expiresIn` (seconds) fields |
| `service/impl/AuthenticationServiceImplTest.java` | Rewritten for the new `AuthenticationManager` / `JwtService` / `RefreshTokenService` collaborators |
| `application.properties` | JWT section repurposed from placeholder to live config; access token lifetime tightened to 15 min (short-lived, refreshed via rotation); added `app.security.cors.allowed-origins` |
| `db/schema.sql` | Added `refresh_tokens` table + DROP statement |

### 11.3 Files removed

- `config/PasswordEncoderConfig.java` — its single `PasswordEncoder` bean was folded into `SecurityConfig`, which now owns the complete authentication configuration (avoids a duplicate-bean conflict and matches the Phase 3 spec's explicit "Configure PasswordEncoder" requirement living in `SecurityConfig`).

### 11.4 pom.xml

**No changes required.** `spring-boot-starter-security` and the `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.6) dependencies were already present from Phase 1.

### 11.5 Security architecture

```
                        ┌─────────────────────────────┐
                        │   Every incoming HTTP call   │
                        └──────────────┬───────────────┘
                                       │
                     ┌─────────────────▼──────────────────┐
                     │   JwtAuthenticationFilter (Order 1) │
                     │   - reads "Authorization: Bearer …" │
                     │   - JwtService.isAccessTokenValid() │
                     │   - loads CustomUserDetails         │
                     │   - populates SecurityContextHolder │
                     └─────────────────┬────────────────────┘
                                       │
                     ┌─────────────────▼──────────────────┐
                     │ UsernamePasswordAuthenticationFilter │
                     │   (Spring Security's default filter, │
                     │    effectively a no-op here since we  │
                     │    never POST form credentials)       │
                     └─────────────────┬────────────────────┘
                                       │
                     ┌─────────────────▼──────────────────┐
                     │      authorizeHttpRequests(...)      │
                     │  path + role rules from SecurityConfig│
                     │  (+ future @PreAuthorize/@Secured)   │
                     └─────────────────┬────────────────────┘
                             ┌─────────┴─────────┐
                       401 Unauthorized     403 Forbidden
                    JwtAuthenticationEntryPoint  JwtAccessDeniedHandler
                             │                         │
                             └───────────┬─────────────┘
                                         │  (both cases)
                                 ErrorResponse JSON
```

- **Stateless**: `SessionCreationPolicy.STATELESS` — no `HttpSession` is ever created or read; every request is independently authenticated from its bearer token.
- **CSRF disabled**: irrelevant for a bearer-token API — CSRF exploits rely on a browser automatically attaching session cookies, which doesn't happen with an `Authorization` header a script must set explicitly.
- **CORS**: configured from `app.security.cors.allowed-origins`, ready for the React/Vite frontend.
- **Password hashing**: `BCryptPasswordEncoder`, bean lives in `SecurityConfig`.
- **Role model**: `ROLE_ADMIN`, `ROLE_SELLER`, `ROLE_CUSTOMER` (unchanged from Phase 1), granted as `GrantedAuthority` objects with the `ROLE_` prefix already baked in from `RoleName`, so `hasRole("ADMIN")` (which implicitly expects/adds the `ROLE_` prefix) lines up correctly.
- **Path-based authorization** is declared in `SecurityConfig` (coarse-grained, applies even before a controller exists); **method-level** `@PreAuthorize` / `@Secured` are enabled via `@EnableMethodSecurity` for controllers to layer on finer-grained checks (e.g. "only the owning seller may edit *this* product") in Phase 4.
- Every path pattern in `SecurityConfig` is **context-path-relative** (e.g. `"/auth/**"`, not `"/api/auth/**"`) because `server.servlet.context-path=/api` already strips that prefix before Spring Security evaluates the request — the externally-visible URL is still `http://host:8080/api/auth/login`.

### 11.6 JWT authentication sequence

```
Client                     AuthenticationController*         AuthenticationServiceImpl        AuthenticationManager / DaoAuthenticationProvider   JwtService        RefreshTokenService
  │  POST /api/auth/login          │                                    │                                     │                          │                    │
  │  {usernameOrEmail, password}   │                                    │                                     │                          │                    │
  ├────────────────────────────────►                                    │                                     │                          │                    │
  │                                │  login(request)                    │                                     │                          │                    │
  │                                ├────────────────────────────────────►                                     │                          │                    │
  │                                │                                    │  authenticate(username, password)   │                          │                    │
  │                                │                                    ├─────────────────────────────────────►                          │                    │
  │                                │                                    │                                     │ loadUserByUsername()     │                    │
  │                                │                                    │                                     │ passwordEncoder.matches()│                    │
  │                                │                                    │  (throws BadCredentialsException on failure, caught & mapped    │                    │
  │                                │                                    │   to UnauthorizedException by AuthenticationServiceImpl)         │                    │
  │                                │                                    ◄─────────────────────────────────────┤                          │                    │
  │                                │                                    │  generateAccessToken(userDetails)   │                          │                    │
  │                                │                                    ├──────────────────────────────────────────────────────────────────►                    │
  │                                │                                    │◄─────────────────────────────────────────────────────────────────┤ access JWT         │
  │                                │                                    │  generateRefreshToken(userDetails)  │                          │                    │
  │                                │                                    ├──────────────────────────────────────────────────────────────────►                    │
  │                                │                                    │◄─────────────────────────────────────────────────────────────────┤ refresh JWT        │
  │                                │                                    │  createRefreshToken(user, token, ttl)                            │                    │
  │                                │                                    ├───────────────────────────────────────────────────────────────────────────────────────►
  │                                │                                    │                                     │                          │  persists RefreshToken row
  │                                │  AuthenticationResponse            │                                     │                          │                    │
  │                                │  {token, refreshToken, expiresIn}  │                                     │                          │                    │
  │                                ◄────────────────────────────────────┤                                     │                          │                    │
  │  200 OK + AuthenticationResponse                                    │                                     │                          │                    │
  ◄────────────────────────────────┤                                    │                                     │                          │                    │

* AuthenticationController is out of scope for Phase 3 (added in Phase 4) - the sequence above documents the
  service-layer contract it will call.

Subsequent authenticated request:
Client                          JwtAuthenticationFilter                CustomUserDetailsService        SecurityContextHolder
  │  GET /api/orders                     │                                       │                              │
  │  Authorization: Bearer <access JWT>  │                                       │                              │
  ├───────────────────────────────────────►                                       │                              │
  │                                      │ extract token, jwtService.isAccessTokenValid()                        │
  │                                      │ extractUsername(token)               │                              │
  │                                      ├───────────────────────────────────────►                              │
  │                                      │◄──────────────────────────────────────┤ CustomUserDetails            │
  │                                      │  build UsernamePasswordAuthenticationToken(userDetails, authorities)  │
  │                                      ├─────────────────────────────────────────────────────────────────────►│
  │                                      │  filterChain.doFilter() continues → authorizeHttpRequests() → (Phase 4 controller)
```

### 11.7 Refresh token sequence

```
Client                       AuthenticationServiceImpl.refreshToken()    JwtService              RefreshTokenService / Repository
  │  POST /api/auth/refresh-token       │                                     │                              │
  │  {refreshToken}                     │                                     │                              │
  ├───────────────────────────────────────►                                     │                              │
  │                                      │  isRefreshTokenStructurallyValid() │                              │
  │                                      ├─────────────────────────────────────►                              │
  │                                      │  (checks signature + expiry + "type":"REFRESH" claim)               │
  │                                      │◄─────────────────────────────────────┤ true/false                  │
  │                                      │  [false] → throw UnauthorizedException → 401 (stop)                │
  │                                      │                                     │                              │
  │                                      │  verifyAndGet(rawToken)                                             │
  │                                      ├──────────────────────────────────────────────────────────────────────►
  │                                      │                                     │  find by token, check revoked/expired
  │                                      │  [not found / revoked / expired] → throw UnauthorizedException → 401 │
  │                                      │◄─────────────────────────────────────────────────────────────────────┤ RefreshToken row
  │                                      │  revokeToken(rawToken)   ── ROTATION: old token can never be reused ─►
  │                                      │  issueTokenPair(user) → generateAccessToken() + generateRefreshToken()
  │                                      │  createRefreshToken(user, newRefreshToken, ttl) ─────────────────────►
  │  200 OK + new {token, refreshToken}  │                                     │                              │
  ◄──────────────────────────────────────┤                                     │                              │

Logout:
Client                       AuthenticationServiceImpl.logout()          RefreshTokenService / Repository
  │  POST /api/auth/logout              │                                     │
  │  {refreshToken}                     │                                     │
  ├───────────────────────────────────────►                                     │
  │                                      │  revokeToken(refreshToken)         │
  │                                      ├─────────────────────────────────────►
  │                                      │                                     │  sets revoked = true (idempotent - no-op if already gone)
  │  204 No Content                      │                                     │
  ◄──────────────────────────────────────┤                                     │

Note: the access token issued before logout remains cryptographically valid until its own short (15-minute)
expiry - this is standard for stateless JWTs. Revocation guarantees the *refresh* token can't mint further
access tokens, bounding a compromised session to at most one remaining access-token lifetime.
```

### 11.8 Checklist — Phase 3 Deliverables

- [x] `SecurityConfig`: `SecurityFilterChain`, `PasswordEncoder` (BCrypt), `AuthenticationManager`, `AuthenticationProvider` (`DaoAuthenticationProvider`), CORS (externalized allow-list), CSRF disabled, `SessionCreationPolicy.STATELESS`, public vs. protected endpoints, role-based authorization via `requestMatchers`
- [x] `JwtService` (access/refresh token generation, username/roles/userId extraction, expiration check, access-token validation) + `JwtTokenProvider` (low-level sign/parse primitives) + JWT utility methods
- [x] `JwtAuthenticationFilter` (`OncePerRequestFilter`: extract → validate → load `UserDetails` → set `SecurityContextHolder`)
- [x] `JwtAuthenticationEntryPoint` (401) + `JwtAccessDeniedHandler` (403), both emitting the shared `ErrorResponse` JSON shape
- [x] `CustomUserDetails` + `CustomUserDetailsService` (load by username **or** email, roles → authorities)
- [x] Full authentication flow: register, login (via `AuthenticationManager`), JWT generation, refresh token generation, refresh-token-exchange API logic, logout (refresh token revocation), BCrypt password encoding
- [x] Role-based authorization for `ROLE_ADMIN` / `ROLE_SELLER` / `ROLE_CUSTOMER` via `requestMatchers` + `@EnableMethodSecurity` (`@PreAuthorize`/`@Secured` ready for Phase 4 controllers)
- [x] Token storage: `RefreshToken` entity, `RefreshTokenRepository`, `RefreshTokenService` + `RefreshTokenServiceImpl` — validation, expiration, and revocation (single-token and revoke-all-for-user)
- [x] Security exceptions: `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, expired/invalid/malformed token handling (all funneled through `JwtAuthenticationFilter`'s catch blocks), unauthorized access handling
- [x] `application.properties` updated: JWT secret/expiration/refresh-expiration (now live, not placeholders), CORS allow-list property
- [x] Tests: `JwtTokenProviderTest`, `JwtServiceTest`, `CustomUserDetailsServiceTest`, `RefreshTokenServiceImplTest`, and a rewritten `AuthenticationServiceImplTest` covering register/login/refresh/logout against the new collaborators
- [x] `pom.xml` reviewed — no changes required (dependencies already present)
- [x] README updated with authentication flow, security architecture, and JWT/refresh-token sequence diagrams
- [ ] Controllers — **Phase 4**
- [ ] React + Vite frontend — later phase

---

**End of Phase 3.** Awaiting instructions for Phase 4.


