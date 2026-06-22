<p align="center">
  <h1 align="center">🛒 eCommerce Microservices Platform</h1>
</p>

<p align="center">
  <strong>Production-grade distributed e-commerce backend — built with Spring Boot 3, Kafka, and Docker</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot" alt="Spring Boot 3.3.4"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue?logo=spring" alt="Spring Cloud"/>
  <img src="https://img.shields.io/badge/Apache%20Kafka-Event%20Driven-black?logo=apachekafka" alt="Kafka"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker" alt="Docker"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

<p align="center">
  <a href="#architecture">Architecture</a> ·
  <a href="#microservices">Services</a> ·
  <a href="#tech-stack">Tech Stack</a> ·
  <a href="#event-flow">Event Flow</a> ·
  <a href="#getting-started">Getting Started</a> ·
  <a href="#api-reference">API Reference</a> ·
  <a href="#observability">Observability</a>
</p>

---

A fully containerized, event-driven e-commerce backend demonstrating real-world microservices patterns: JWT-secured API Gateway, service discovery, centralized config management, Kafka-based async communication with Avro schema enforcement, distributed tracing, and per-service PostgreSQL databases — all orchestrated with Docker Compose.

---

## Architecture

```
                            ┌──────────────────────────┐
                            │       API Gateway         │
                            │   :9000 (Spring Cloud)    │
                            │  JWT Auth · Role Filter   │
                            │  Global Logging Filter    │
                            └──────────┬───────────────┘
                                       │
              ┌────────────────────────┼──────────────────────────┐
              ▼                        ▼                           ▼
    ┌─────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
    │  Order Service  │    │  Inventory Service  │    │  Shipping Service   │
    │     :9021       │    │       :9020         │    │       :9030         │
    │   (order-db)    │    │  (inventory-db)     │    │   (shipping-db)     │
    └────────┬────────┘    └──────────┬──────────┘    └─────────────────────┘
             │                        │
             │   Kafka Events (Avro)  │
             ▼                        ▼
    ┌────────────────────────────────────────────┐
    │              Apache Kafka + Schema Registry │
    │   order-created ──────────────────────────► inventory-service (reduce stock)
    │   order-status-updated ◄─────────────────── inventory-service (FULFILLED / OUT_OF_STOCK)
    │   order-created + order-status-updated ───► notification-service (log/notify)
    └────────────────────────────────────────────┘
             │
             ▼
    ┌─────────────────────┐    ┌──────────────────┐    ┌─────────────────┐
    │  Notification Svc   │    │  Discovery Svc   │    │  Config Server  │
    │       :9040         │    │  (Eureka) :8761  │    │     :8888       │
    └─────────────────────┘    └──────────────────┘    └─────────────────┘
             │
             ▼
    ┌─────────────────────┐
    │  Zipkin (Tracing)   │
    │        :9411        │
    └─────────────────────┘
```

---

## Microservices

| Service | Port | Responsibility | Key Tech |
|:--------|:----:|:---------------|:---------|
| **API Gateway** | 9000 | Single entry point — JWT validation, role-based routing, global request/response logging | Spring Cloud Gateway, JJWT 0.12.6 |
| **Order Service** | 9021 | Create/manage orders, publish `OrderCreatedEvent` via Kafka, consume `OrderStatusUpdatedEvent` to sync status | Spring Data JPA, Kafka Producer/Consumer, Avro |
| **Inventory Service** | 9020 | Stock management, consumes `OrderCreatedEvent` to reduce stock, publishes `OrderStatusUpdatedEvent` (FULFILLED / OUT_OF_STOCK) | Spring Data JPA, Kafka Producer/Consumer, OpenFeign |
| **Shipping Service** | 9030 | Handles shipment records; called synchronously by Order Service via Feign | Spring Data JPA, ModelMapper |
| **Notification Service** | 9040 | Listens to both Kafka topics and logs order/status events (extensible to email/SMS) | Spring Kafka, Avro |
| **Discovery Service** | 8761 | Eureka server — service registry for all microservices | Spring Cloud Netflix Eureka |
| **Config Server** | 8888 | Centralized configuration via Git-backed repo with retry and fail-fast support | Spring Cloud Config Server |

---

## Tech Stack

### Core
- **Java 21** — LTS with modern language features
- **Spring Boot 3.3.4** — auto-configuration, production-ready defaults
- **Spring Cloud 2023.0.3** — Gateway, Config, Eureka, OpenFeign, Resilience4j

### Messaging
- **Apache Kafka** (KRaft mode, no Zookeeper) — event streaming
- **Confluent Schema Registry** — Avro schema enforcement across producers/consumers
- **Apache Avro** — strongly-typed, schema-evolved event contracts (`OrderCreatedEvent`, `OrderStatusUpdatedEvent`)

### Data
- **PostgreSQL 15** — one dedicated database per service (true data isolation)
- **Spring Data JPA / Hibernate** — ORM with `@Transactional` stock deduction

### Infrastructure
- **Docker Compose** — full stack orchestration with health checks and dependency ordering
- **Spring Cloud API Gateway** — reactive gateway with custom `GatewayFilterFactory`
- **Spring Cloud Config** — externalized config from Git with `@RefreshScope` hot-reload
- **Netflix Eureka** — service registry and client-side load balancing

### Observability
- **Micrometer + Brave** — distributed trace instrumentation
- **Zipkin** — trace visualization across all services
- **Logback** — structured logging with `traceId` and `spanId` embedded in every log line, rolling file appenders (10 MB / 30 days)
- **Spring Boot Actuator** — health endpoints and metrics

### Security
- **JJWT 0.12.6** — JWT parsing in the API Gateway
- Custom `AuthenticationGatewayFilterFactory` — validates Bearer tokens, extracts `userId` and `roles`, forwards as `X-User-Id` / `X-User-Roles` headers to downstream services

---

## Event Flow

Order placement triggers a fully asynchronous, event-driven pipeline:

```
POST /orders/core/create-order
         │
         ▼
   Order Service
   ─ saves order with status PENDING
   ─ publishes OrderCreatedEvent (Avro) → Kafka [order-created, 3 partitions]
         │
         ├──► Inventory Service (consumer group: inventory)
         │     ─ reduces stock transactionally
         │     ─ if stock OK  → publishes OrderStatusUpdatedEvent { status: "FULFILLED" }
         │     ─ if stock low → publishes OrderStatusUpdatedEvent { status: "OUT_OF_STOCK" }
         │
         └──► Notification Service (consumer group: notification)
               ─ logs order event (hook for email/SMS/push)

   Kafka [order-status-updated]
         │
         ├──► Order Service
         │     ─ updates order status in DB (PENDING → FULFILLED / OUT_OF_STOCK)
         │
         └──► Notification Service
               ─ logs status change
```

**Avro schemas** define the event contracts — breaking changes are caught at the Schema Registry before reaching consumers.

---

## Getting Started

### Prerequisites
- Docker Desktop (with Compose v2)
- Git

### Clone and Run

```bash
git clone https://github.com/<your-username>/ecommerce-microservices.git
cd ecommerce-microservices
docker compose up --build
```

Docker Compose starts services in dependency order with health checks:

1. Zipkin, Kafka (KRaft), Schema Registry, PostgreSQL instances
2. Discovery Service (Eureka)
3. Config Server (waits for Eureka to be healthy)
4. Inventory, Order, Shipping, Notification Services (wait for Config Server + DB + Kafka)
5. API Gateway (waits for business services)

> **First build:** Maven downloads dependencies inside containers — allow 5–10 minutes on a fresh machine.

### Verify Everything Is Up

```bash
# Eureka dashboard — all services should appear registered
open http://localhost:8761

# Zipkin trace UI
open http://localhost:9411

# Kafka Schema Registry subjects
curl http://localhost:8081/subjects

# Inventory service health
curl http://localhost:9000/inventory/products
```

### Sample API Calls (via API Gateway on port 9000)

**Create an Order:**
```bash
curl -X POST http://localhost:9000/orders/core/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "deliveryAddress": "123 Main Street, Gaya, Bihar",
    "items": [
      { "productId": 1, "quantity": 2 },
      { "productId": 5, "quantity": 1 }
    ]
  }'
```

**Get All Products (Inventory):**
```bash
curl http://localhost:9000/inventory/products \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Check Order Status:**
```bash
curl http://localhost:9000/orders/core/{orderId} \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Cancel an Order:**
```bash
curl -X PUT http://localhost:9000/orders/core/cancel-order/{orderId} \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## API Reference

### Order Service (`/orders`)

| Method | Endpoint | Description |
|:-------|:---------|:------------|
| `POST` | `/core/create-order` | Place a new order; triggers Kafka event pipeline |
| `GET` | `/core` | List all orders |
| `GET` | `/core/{id}` | Get order by ID |
| `PUT` | `/core/cancel-order/{id}` | Cancel an order |

### Inventory Service (`/inventory`)

| Method | Endpoint | Description |
|:-------|:---------|:------------|
| `GET` | `/products` | List all products with stock levels |
| `GET` | `/products/{id}` | Get single product |
| `PUT` | `/products/reduce-stocks` | Reduce stock (called internally / via Kafka) |
| `PUT` | `/products/restock` | Restock items |

### Shipping Service (`/shipping`)

| Method | Endpoint | Description |
|:-------|:---------|:------------|
| `POST` | `/orders/ship` | Initiate shipment for an order |
| `GET` | `/orders` | List all shipments |
| `GET` | `/orders/order/{orderId}` | Get shipping record by order ID |

---

## Security — API Gateway Auth Filter

All routes pass through the `AuthenticationGatewayFilterFactory`. The filter:

1. Reads the `Authorization: Bearer <token>` header
2. Parses and validates the JWT using HMAC-SHA secret key (JJWT 0.12.6)
3. Extracts `userId` (from JWT subject) and `roles` (from JWT claims)
4. Checks roles against `allowedRoles` config per route — returns `403` if unauthorized
5. Forwards `X-User-Id` and `X-User-Roles` headers to downstream services

Downstream services trust these headers — no repeated JWT parsing across the cluster.

---

## Observability

### Distributed Tracing

Every service is instrumented with **Micrometer + Brave**. Each request generates a `traceId` and `spanId` that flow through HTTP calls (via Feign) and Kafka messages. Traces are exported to **Zipkin** at `http://localhost:9411`.

Log pattern embedded in Logback across all services:
```
%d{dd-MM-yyyy HH:mm:ss.SSS} [%thread] [%X{traceId}-%X{spanId}] %-5level <service>-%logger{36}.%M - %msg%n
```

### Logging

Each service writes structured logs to:
- **Console** (stdout) — for Docker log aggregation
- **Rolling file** (`logs/<service-name>/application-yyyy-MM-dd.N.log`) — 10 MB per file, 30-day retention

### Health Checks

All services expose `GET /actuator/health` — used by Docker Compose for startup dependency ordering.

---

## Dynamic Config with `@RefreshScope`

The **Config Server** reads from a Git-backed config repository. The Order Service uses `@RefreshScope` on controllers and config beans — calling `POST /actuator/refresh` reloads values like `features.user-tracking-enabled` and `my.variable` at runtime without restarting the container.

---

## Kafka Infrastructure Details

| Topic | Partitions | Producers | Consumers |
|:------|:----------:|:----------|:----------|
| `order-created` | 3 | Order Service | Inventory Service, Notification Service |
| `order-status-updated` | 3 | Inventory Service | Order Service, Notification Service |

- **KRaft mode** — no Zookeeper dependency
- **Confluent Schema Registry** enforces Avro schemas, prevents breaking event changes
- Messages are keyed by `orderId` — ensures ordering per order within partitions

---

## Project Structure

```
ecommerce-microservices/
├── api-gateway/              # Spring Cloud Gateway + JWT filter
├── config-server/            # Spring Cloud Config Server (Git-backed)
├── discovery-service/        # Netflix Eureka Server
├── order-service/            # Order CRUD + Kafka producer/consumer
├── inventory-service/        # Stock management + Kafka consumer/producer
├── shipping-service/         # Shipment records
├── notification-service/     # Event logging (Kafka consumer)
└── docker-compose.yml        # Full stack orchestration
```

Each service follows the same internal layout:
```
<service>/
├── src/main/java/
│   └── com/codingshuttle/ecommerce/<service>/
│       ├── config/           # Spring beans, Kafka topics, feature flags
│       ├── controller/       # REST endpoints
│       ├── service/          # Business logic
│       ├── entity/           # JPA entities
│       ├── dto/              # Request/Response DTOs
│       ├── repository/       # Spring Data JPA repositories
│       └── consumer/         # Kafka listeners (where applicable)
├── src/main/resources/
│   └── avro/                 # Avro schema definitions
├── Dockerfile                # Maven multi-stage build (eclipse-temurin-21-alpine)
└── pom.xml
```

---

## What This Project Demonstrates

| Concept | Implementation |
|:--------|:--------------|
| API Gateway pattern | Spring Cloud Gateway with custom `AbstractGatewayFilterFactory` |
| Service discovery | Netflix Eureka with client-side load balancing via OpenFeign |
| Centralized config | Spring Cloud Config Server backed by Git, with `@RefreshScope` hot-reload |
| Event-driven architecture | Kafka + Avro with Schema Registry — typed, schema-versioned events |
| Database-per-service | Three separate PostgreSQL instances (order-db, inventory-db, shipping-db) |
| Distributed tracing | Micrometer Brave → Zipkin, `traceId/spanId` in every log line |
| Containerized deployment | Full Docker Compose stack with health checks and dependency ordering |
| Reactive gateway | Spring WebFlux-based API Gateway with `ServerWebExchange` request mutation |
| Transactional stock deduction | `@Transactional` stock reduce/restock in Inventory Service |
| Role-based access control | JWT claims extraction + role filtering at the gateway layer |

---

## License

MIT — free to use, fork, and build upon.

---

<p align="center">
  Built with ☕ Java 21 · Spring Boot 3 · Apache Kafka · Docker
</p>
