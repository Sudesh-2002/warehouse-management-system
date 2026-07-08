![CI](https://github.com/Sudesh-2002/warehouse-management-system/actions/workflows/ci.yml/badge.svg)

# Warehouse Order & Inventory Management System

A backend system for distributors managing stock across multiple warehouses and fulfilling customer orders — modeled after real-world foodservice distribution workflows (inventory tracking, order lifecycle management, tiered pricing).

## Overview

This project simulates core operations of a distribution business:
- Tracking product stock across multiple warehouses
- Placing customer orders with multiple line items
- Preventing overselling through safe, concurrency-aware stock deduction
- Applying quantity-based bulk discount pricing
- Managing order status transitions (PENDING → CONFIRMED → SHIPPED / CANCELLED)

## Tech Stack

- **Java 21** / **Spring Boot 4.1**
- **Spring Data JPA** + **PostgreSQL**
- **Bean Validation** (`@Valid`) for request validation
- **JUnit 5** + **Mockito** for unit tests
- **Testcontainers** for integration tests against a real PostgreSQL instance
- **Swagger / OpenAPI 3** for interactive API documentation
- **GitHub Actions** for CI (tests run automatically on every push/PR)
- **Maven**

## Architecture & Design Decisions

- **Strategy pattern for pricing** — `PricingStrategy` interface with `TieredQuantityDiscountStrategy` implementation, so new pricing rules (loyalty discounts, seasonal promotions) can be added without modifying `OrderService` (Open/Closed Principle).
- **Pessimistic locking on stock deduction** — prevents race conditions where two concurrent orders could both read stale stock and oversell the same inventory.
- **Price snapshotting** — `OrderLineItem` stores `unitPriceAtOrderTime`, so historical orders remain accurate even if a product's price changes later.
- **Transactional order confirmation** — if any line item in an order has insufficient stock, the entire confirmation rolls back; no partial stock deduction ever occurs.
- **DTOs throughout** — entities are never exposed directly via the API; every endpoint uses dedicated request/response DTOs with validation.
- **Global exception handling** — a centralized `@RestControllerAdvice` maps domain exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, `InsufficientStockException`, `InvalidOrderStatusException`) to consistent, correctly-coded HTTP responses (404, 409, 400).

## Core Domain

| Entity | Description |
|---|---|
| Product | SKU, name, category, unit price, unit of measure |
| Warehouse | Location, capacity |
| Inventory | Stock levels per product, per warehouse |
| Customer | Basic profile |
| Order / OrderLineItem | Order header + line items, status lifecycle |

## API Endpoints

| Resource | Endpoints |
|---|---|
| Products | `POST /api/products`, `GET /api/products`, `GET /api/products/{id}`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` |
| Warehouses | `POST /api/warehouses`, `GET /api/warehouses`, `GET /api/warehouses/{id}`, `PUT /api/warehouses/{id}`, `DELETE /api/warehouses/{id}` |
| Customers | `POST /api/customers`, `GET /api/customers`, `GET /api/customers/{id}`, `PUT /api/customers/{id}`, `DELETE /api/customers/{id}` |
| Inventory | `POST /api/inventory`, `POST /api/inventory/restock`, `POST /api/inventory/deduct`, `GET /api/inventory`, `GET /api/inventory/product/{productId}/warehouse/{warehouseId}` |
| Orders | `POST /api/orders`, `POST /api/orders/{id}/confirm`, `POST /api/orders/{id}/ship`, `POST /api/orders/{id}/cancel`, `GET /api/orders`, `GET /api/orders/{id}` |

Full interactive documentation is available via Swagger UI once the app is running (see below).

## Getting Started

### Prerequisites
- Java 21
- Maven
- PostgreSQL (running locally, or via Docker)
- Docker Desktop (only required to run the integration test suite)

### Setup

1. Create a PostgreSQL database:
```sql
   CREATE DATABASE warehouse_db;
```

2. Configure `src/main/resources/application.properties` with your database credentials.

3. Run the application:
```bash
   mvn spring-boot:run
```

4. API is available at `http://localhost:8080`. Interactive docs at `http://localhost:8080/swagger-ui/index.html`.

### Running Tests

```bash
mvn test
```

This runs the full suite: Mockito-based unit tests for business logic (inventory deduction, pricing tiers, order status transitions) and Testcontainers-based integration tests that spin up a real, disposable PostgreSQL container to verify the full request → controller → service → database flow.

## Example: Placing and Confirming an Order

```bash
# Place an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "lineItems": [
      { "productId": 1, "warehouseId": 1, "quantity": 60 }
    ]
  }'

# Confirm it (deducts stock; rejects with 409 if insufficient)
curl -X POST http://localhost:8080/api/orders/1/confirm
```

A 60-unit order automatically applies a 10% bulk discount tier via the pricing strategy.

## Author

**Sudesh Hansika**
[GitHub](https://github.com/Sudesh-2002) · [LinkedIn](https://linkedin.com/in/sudesh-hansika-4a9794320) · [Portfolio](https://personal-portfolio-six-smoky-79.vercel.app)