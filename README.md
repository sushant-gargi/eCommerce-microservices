<p align="center">
  <h1 align="center">🛒 eCommerce Microservices Platform</h1>
  <p align="center">
    <strong>Production-grade distributed eCommerce backend built with Spring Boot 3, Kafka, and Docker</strong>
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.3-brightgreen?logo=spring" alt="Spring Cloud"/>
  <img src="https://img.shields.io/badge/Apache%20Kafka-7.7.1-black?logo=apachekafka" alt="Kafka"/>
  <img src="https://img.shields.io/badge/Docker-Compose-blue?logo=docker" alt="Docker"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

<p align="center">
  <a href="#architecture">Architecture</a> ·
  <a href="#services">Services</a> ·
  <a href="#tech-stack">Tech Stack</a> ·
  <a href="#event-driven-flow">Event Flow</a> ·
  <a href="#resilience-patterns">Resilience</a> ·
  <a href="#quick-start">Quick Start</a> ·
  <a href="#api-reference">API Reference</a>
</p>

---

## What This Project Demonstrates

A **fully containerized**, event-driven eCommerce backend that showcases production-level backend engineering skills:

| Capability | Implementation |
|:---|:---|
| **Microservices Architecture** | 6 independent Spring Boot services with isolated databases |
| **Event-Driven Communication** | Apache Kafka with Avro schema enforcement via Confluent Schema Registry |
| **API Security** | JWT authentication + role-based access control at the API Gateway |
| **Resilience Engineering** | Circuit Breakers, Retry, Rate Limiting via Resilience4j |
| **Distributed Tracing** | Full trace propagation with Zipkin + Micrometer |
| **Centralized Config** | Spring Cloud Config Server backed by a Git repository |
| **Service Discovery** | Netflix Eureka for dynamic service registration and load balancing |
| **Containerization** | Full Docker Compose orchestration with health checks and startup ordering |

---

## Architecture

```
                          ┌─────────────────────────────────────┐
                          │         CLIENT (REST API)            │
                          └──────────────┬──────────────────────┘
                                         │
                          ┌──────────────▼──────────────────────┐
                          │           API GATEWAY  :9000         │
                          │  JWT Auth · Role-Based Routing       │
                          │  GlobalLoggingFilter · StripPrefix   │
                          └──────┬────────────┬─────────┬───────┘
                                 │            │         │
               ┌─────────────────▼──┐  ┌──────▼───┐  ┌─▼─────────────┐
               │  ORDER SERVICE     │  │INVENTORY │  │SHIPPING       │
               │  :9021             │  │SERVICE   │  │SERVICE :9030  │
               │  [USER, ADMIN]     │  │:9020     │  │[USER, ADMIN]  │
               │                   │  │[ADMIN]   │  │               │
               └──────┬────────────┘  └──────────┘  └───────────────┘
                      │  Kafka Events
         ┌────────────▼────────────────────┐
         │         KAFKA + SCHEMA REGISTRY  │
         │  order_created · order_status_updated  │
         └───────────┬──────────────────────┘
                     │  Consumes events
         ┌───────────▼──────────┐   ┌──────────────────────┐
         │  INVENTORY SERVICE   │   │  NOTIFICATION SERVICE │
         │  (stock management)  │   │  (audit logging)      │
         └──────────────────────┘   └──────────────────────┘

  ┌─────────────────────────────────────────────────────────────┐
  │              INFRASTRUCTURE LAYER                            │
  │  Config Server :8888 · Discovery Service :8761 · Zipkin :9411│
  └─────────────────────────────────────────────────────────────┘
```

---

## Services

### 🔀 API Gateway — Port 9000
The single entry point for all client traffic. Built on Spring Cloud Gateway (reactive WebFlux).

- **JWT Authentication Filter** — validates Bearer tokens, extracts `userId` and `roles`, forwards as `X-User-Id` / `X-User-Roles` headers to downstream services
- **Role-based routing** — `ORDER` and `SHIPPING` services require `USER` or `ADMIN`; `INVENTORY` management restricted to `ADMIN` only
- **Custom filters** — `LoggingOrdersFilter` for per-route logging; `GlobalLoggingFilter` for request/response lifecycle tracing
- **Config-server integration** — routes, JWT secret, and Eureka registration all pulled from the central config repo

### 📦 Order Service — Port 9021
Core business service. Handles the full order lifecycle.

- Creates orders by **synchronously calling Inventory** (via OpenFeign) to reduce stock and **Shipping** to initiate delivery
- Publishes `OrderCreatedEvent` to Kafka on successful order placement
- Listens for `OrderStatusUpdatedEvent` from Inventory to reflect fulfillment or out-of-stock state
- Supports order cancellation with automatic inventory restocking via Feign
- **Resilience4j** configuration: retry (3 attempts, 200ms wait), circuit breaker (10-call sliding window, 50% failure threshold, 1s open state), and rate limiter (100 req/s) on inventory calls

### 🗃️ Inventory Service — Port 9020
Manages product catalog and stock levels with a seeded dataset of 20 products.

- Exposes REST endpoints for listing products, reducing stock on orders, and restocking on cancellations
- **Kafka Consumer** — listens on `order_created` topic; reduces stock and publishes `OrderStatusUpdatedEvent` (`FULFILLED` or `OUT_OF_STOCK`)
- Cross-service Feign client to Order Service for inter-service communication demonstration
- Seeded with a `data.sql` script (smartphones, laptops, VR headsets, and more)

### 🚚 Shipping Service — Port 9030
Handles shipping record creation and tracking.

- Accepts shipping requests from Order Service via synchronous Feign call
- Persists shipping records to its own dedicated PostgreSQL database
- Returns shipping status and confirmation to Order Service

### 🔔 Notification Service — Port 9040
Purely event-driven notification handler — no REST endpoints.

- Consumes both `order_created` and `order_status_updated` Kafka topics
- Logs structured order lifecycle notifications (extensible to email/SMS integrations)
- Demonstrates a **fanout event consumer** pattern where multiple services react to the same event

### 🔍 Discovery Service — Port 8761
Netflix Eureka Server for service registry and client-side load balancing. All services register on startup and use `lb://service-name` URIs.

### ⚙️ Config Server — Port 8888
Spring Cloud Config Server backed by the [ecommerce-config-server](https://github.com/sushant-gargi/ecommerce-config-server) Git repository.

- Serves per-service YAML/properties files centrally
- Supports **environment profiles** (`dev`, `prod`) — Order Service has a `order-service-dev.properties` override
- `@RefreshScope` beans in Order Service pick up live config changes without restarts

---

## Tech Stack

| Layer | Technology |
|:---|:---|
| **Language & Runtime** | Java 21, Spring Boot 3.3.4 |
| **Service Mesh** | Spring Cloud 2023.0.3, Netflix Eureka, Spring Cloud Gateway |
| **Messaging** | Apache Kafka (KRaft mode), Confluent Schema Registry, Apache Avro |
| **Persistence** | Spring Data JPA, PostgreSQL 15 (separate DB per service) |
| **Resilience** | Resilience4j (Circuit Breaker, Retry, Rate Limiter) |
| **Security** | JWT (jjwt 0.12.6), HMAC-SHA256 |
| **Service Communication** | OpenFeign with Micrometer instrumentation |
| **Observability** | Micrometer Tracing, Zipkin, structured Logback (traceId/spanId in every log line) |
| **Configuration** | Spring Cloud Config Server (Git-backed) |
| **Containerization** | Docker, Docker Compose (multi-container with health checks) |
| **Build** | Maven 3.9.4, Eclipse Temurin 21 Alpine images |

---

## Event-Driven Flow

```
  Client
    │
    ├─ POST /api/v1/orders/core/create-order
    │
  Order Service
    ├─ Feign → Inventory Service (reduce stocks) ──── Resilience4j [retry + circuit breaker]
    ├─ Feign → Shipping Service (ship order)
    ├─ Persist Order (PENDING)
    └─ Publish ──► Kafka: order_created (Avro)
                          │
            ┌─────────────┼────────────────┐
            │                              │
  Inventory Service                Notification Service
  Consumes order_created          Consumes order_created
  Reduces stock                   Logs order event
  Publishes ──► order_status_updated
                          │
                  Order Service
                  Consumes status update
                  Updates Order → FULFILLED / OUT_OF_STOCK
                          │
                  Notification Service
                  Consumes status update
                  Logs fulfillment event
```

**Avro Schemas** enforce message contracts across producers and consumers:

```json
// OrderCreatedEvent
{ "orderId": long, "deliveryAddress": string, "items": [{ "productId": long, "quantity": int }] }

// OrderStatusUpdatedEvent  
{ "orderId": long, "orderStatus": string }
```

---

## Resilience Patterns

The Order Service implements a layered resilience strategy for downstream calls:

```
Resilience4j on inventoryClient:
  ├── Rate Limiter:    100 req/s, 10ms timeout
  ├── Retry:          3 attempts, 200ms wait
  └── Circuit Breaker:
        sliding window: 10 calls (COUNT_BASED)
        failure threshold: 50%
        open state wait: 1s
        half-open probe: 3 calls
```

---

## Distributed Tracing

Every log line includes `traceId` and `spanId` via Logback pattern:

```
22-06-2026 12:30:45.123 [reactor-http] [a1b2c3d4-e5f6] INFO order-service - Order created: id=42
```

Traces are exported to **Zipkin** at `http://localhost:9411` with 100% sampling rate, giving full request graphs across Gateway → Order → Inventory → Shipping.

---

## Quick Start

**Prerequisites:** Docker and Docker Compose installed.

```bash
# 1. Clone the repository
git clone https://github.com/sushant-gargi/eCommerce-microservices.git
cd eCommerce-microservices

# 2. Start the full stack
docker compose up --build

# Services start in dependency order:
#   Zipkin → Kafka → Schema Registry → Databases →
#   Discovery → Config → Services → API Gateway
```

Once running, services are accessible at:

| Service | URL |
|:---|:---|
| API Gateway | http://localhost:9000 |
| Discovery (Eureka UI) | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Zipkin Tracing UI | http://localhost:9411 |
| Schema Registry | http://localhost:8081 |
| Inventory Service | http://localhost:9020 |
| Order Service | http://localhost:9021 |
| Shipping Service | http://localhost:9030 |
| Notification Service | http://localhost:9040 |

---

## API Reference

All requests through the **API Gateway** require a valid JWT Bearer token (except for admin-only inventory endpoints, which require `ADMIN` role).

```http
Authorization: Bearer <jwt-token>
```

### Orders  `[USER, ADMIN]`

```http
POST   /api/v1/orders/core/create-order     # Create a new order
GET    /api/v1/orders/core                  # List all orders
GET    /api/v1/orders/core/{id}             # Get order by ID
PUT    /api/v1/orders/core/cancel-order/{id}# Cancel an order (restocks inventory)
```

**Create Order Request:**
```json
{
  "deliveryAddress": "123 Main St, Mumbai",
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 5, "quantity": 1 }
  ]
}
```

### Inventory  `[ADMIN only]`

```http
GET  /api/v1/inventory/products             # List all products
GET  /api/v1/inventory/products/{id}        # Get product by ID
PUT  /api/v1/inventory/products/reduce-stocks
PUT  /api/v1/inventory/products/restock
```

### Shipping  `[USER, ADMIN]`

```http
POST /api/v1/shipping/orders/ship           # Ship an order
GET  /api/v1/shipping/orders                # List all shipments
GET  /api/v1/shipping/orders/{id}           # Get shipment by ID
```

---

## Project Structure

```
eCommerce-microservices/
├── api-gateway/            # JWT auth, routing, global filters
├── config-server/          # Spring Cloud Config (Git-backed)
├── discovery-service/      # Netflix Eureka Server
├── inventory-service/      # Product catalog, stock management
│   └── resources/avro/     # Avro schema definitions
├── notification-service/   # Event consumer for notifications
├── order-service/          # Order lifecycle, Feign clients, Kafka producer
│   └── resources/avro/     # Avro schema definitions
├── shipping-service/       # Shipping record management
├── logs/                   # Mounted log volumes (per-service rolling logs)
└── docker-compose.yml      # Full stack orchestration
```

---

## Key Design Decisions

**Database per Service** — Each service owns its PostgreSQL schema, enforcing bounded context isolation and preventing tight coupling via shared databases.

**Avro + Schema Registry** — Kafka messages use Avro serialization with a central Schema Registry, ensuring backward/forward-compatible schema evolution across producer and consumer upgrades.

**Synchronous + Async hybrid** — Stock reduction on order creation happens synchronously (Feign) for immediate consistency; status propagation is asynchronous (Kafka) for loose coupling and resilience.

**Centralized Externalized Config** — All environment-specific config lives in a separate Git repo (`ecommerce-config-server`), making environment promotion (dev → prod) a config-only change with zero code modification.

**`@RefreshScope`** — Feature flags (e.g., `user-tracking-enabled`) can be toggled live via Spring Actuator `/actuator/refresh` without service restart.

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  <sub>Built with ☕ Java 21 · Spring Boot 3 · Kafka · Docker</sub>
</p>
