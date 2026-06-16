# Food Delivery Microservices Platform

A production-grade backend platform for food delivery built on a microservices architecture — manages orders and payments through a fully distributed infrastructure with service discovery, centralized routing, resilience patterns, and stateless JWT authentication.

---

## Table of Contents

- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Running Locally](#running-locally)
- [Testing](#testing)
- [API Endpoints](#api-endpoints)
- [Key Concepts](#key-concepts)
- [Contributing](#contributing)
- [License](#license)

---

## Architecture

```
┌────────────┐         ┌──────────────────┐         ┌────────────────────────────────┐
│            │         │                  │         │         Eureka Server          │
│   Client   │────────▶│   API Gateway    │────────▶│       (Service Discovery)      │
│            │         │   :8082          │         │           :8081                │
└────────────┘         └────────┬─────────┘         └────────────────────────────────┘
                                │                              ▲       ▲
                                │  routes via service name     │       │
                    ┌───────────┼───────────┐          register│       │register
                    │           │           │                  │       │
                    ▼           ▼           ▼                  │       │
          ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │       │
          │    Auth      │ │  Pagamentos │ │   Pedidos   │    │       │
          │  (JWT Auth)  │ │ (Payments)  │ │  (Orders)   │────┘       │
          │              │ │   :8083     │ │   :8084     │            │
          └─────────────┘ └──────┬──────┘ └──────┬──────┘            │
                                 │               │                    │
                                 │  OpenFeign    │                    │
                                 │◀──────────────┘                    │
                                 │                                    │
                                 ▼                                    │
                          ┌─────────────┐                             │
                          │   MySQL 8.0 │                             │
                          │  (per-svc)  │─────────────────────────────┘
                          └─────────────┘
```

**Request flow:** Client → Gateway (routing + JWT validation) → Eureka (service lookup) → Target Microservice → MySQL

---

## Project Structure

```
java-spring-repo/
├── server/              # Eureka Service Discovery
│   └── src/
├── gateway/             # Spring Cloud Gateway (routing + security)
│   └── src/
├── auth/                # JWT Authentication service
│   └── src/
├── pedidos/             # Order management microservice
│   └── src/
├── pagamentos/          # Payment processing microservice
│   └── src/
├── docker-compose.yml   # Full-stack orchestration
└── README.md
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Runtime (Eclipse Temurin) |
| Spring Boot | 3.5.0 | Application framework |
| Spring Cloud | 2025.0.1 | Microservices infrastructure |
| Eureka | (Spring Cloud) | Service Discovery |
| Spring Cloud Gateway | (Spring Cloud) | API Gateway |
| Resilience4j | 2.3.0 | Circuit Breaker & Fallback |
| Hibernate / JPA | (Spring Boot) | ORM & persistence |
| Flyway | (Spring Boot managed) | Database migrations |
| MySQL | 8.0 | Production database |
| Docker | latest | Containerization |
| Maven | wrapper included | Build tool |

---

## Requirements

| Requirement | Notes |
|---|---|
| **Java 25** | Eclipse Temurin recommended |
| **Docker & Docker Compose** | Required for containerized execution |
| **MySQL 8.0** | Required only for local (non-Docker) execution |
| **Maven** | Wrapper (`./mvnw`) included in each service |

### Compatibility

- **OS:** Linux, macOS, Windows (WSL2 recommended)
- **Docker Compose:** v2+ (uses `docker-compose.yml` v3 syntax)
- **JDK:** Tested with Eclipse Temurin 25; other OpenJDK distributions should work

---

## Quick Start

The fastest way to get the full platform running:

```bash
# Clone the repository
git clone https://github.com/joliveira-abaqus/java-spring-repo.git
cd java-spring-repo

# Start all services with Docker Compose
docker-compose up --build
```

Once containers are healthy:

| Endpoint | URL |
|---|---|
| Eureka Dashboard | http://localhost:8081 |
| API Gateway | http://localhost:8082 |
| Payments (direct) | http://localhost:8083 |
| Orders (direct) | http://localhost:8084 |

### Verify the setup

```bash
# Check Eureka registered services
curl http://localhost:8081/eureka/apps

# Create a payment via Gateway
curl -X POST http://localhost:8082/pagamentos-ms/pagamentos \
  -H "Content-Type: application/json" \
  -d '{"valor": 50.00, "nome": "John", "numero": "1234", "expiracao": "12/2030", "codigo": "123", "pedidoId": 1, "formaDePagamento": "CREDIT_CARD"}'
```

---

## Running Locally

When running without Docker, start services in this order (each in a separate terminal):

```bash
# 1. Eureka Server (must start first)
cd server && ./mvnw spring-boot:run

# 2. API Gateway
cd gateway && ./mvnw spring-boot:run

# 3. Auth Service
cd auth && ./mvnw spring-boot:run

# 4. Payments (requires local MySQL on port 3306)
cd pagamentos && ./mvnw spring-boot:run

# 5. Orders (requires local MySQL on port 3306)
cd pedidos && ./mvnw spring-boot:run
```

> **Note:** When running locally, `pagamentos` and `pedidos` register with Eureka on dynamically assigned ports. Access them through the Gateway for proper routing.

---

## Testing

### Test Suites

| Service | Tests | Type |
|---|---|---|
| `pagamentos` | 22 | Unit + Integration |
| `pedidos` | 19 | Unit + Integration |

### Run tests

```bash
# Payments service tests
cd pagamentos && ./mvnw test

# Orders service tests
cd pedidos && ./mvnw test

# Run all tests (from project root)
cd pagamentos && ./mvnw test && cd ../pedidos && ./mvnw test
```

> Integration tests use an **in-memory H2 database** (profile `test`) — no MySQL instance required.

---

## API Endpoints

All requests should go through the Gateway (`http://localhost:8082`):

| Method | Path | Service | Description |
|---|---|---|---|
| `POST` | `/pagamentos-ms/pagamentos` | Payments | Create a payment |
| `GET` | `/pagamentos-ms/pagamentos` | Payments | List all payments |
| `GET` | `/pagamentos-ms/pagamentos/{id}` | Payments | Get payment by ID |
| `PATCH` | `/pagamentos-ms/pagamentos/{id}/confirmar` | Payments | Confirm a payment |
| `DELETE` | `/pagamentos-ms/pagamentos/{id}` | Payments | Cancel a payment |
| `GET` | `/pedidos-ms/pedidos` | Orders | List all orders |
| `GET` | `/pedidos-ms/pedidos/{id}` | Orders | Get order by ID |

---

## Key Concepts

### Service Discovery (Eureka)
All microservices register themselves with the Eureka Server on startup. The Gateway resolves service names to actual host:port via Eureka, enabling dynamic scaling without hardcoded URLs.

### API Gateway
Single entry point for all client requests. Handles routing (by service name), JWT token validation, and request filtering. Communicates the authenticated user role downstream via the `X-Auth-User-Role` header.

### Circuit Breaker & Fallback (Resilience4j)
Payment confirmations use a Circuit Breaker pattern. When the Orders service is unavailable, the fallback marks the payment as `CONFIRMADO_SEM_INTEGRACAO` (confirmed without integration) to prevent cascading failures.

### Synchronous Communication (OpenFeign)
Inter-service calls (e.g., Payments → Orders status update) use declarative OpenFeign clients, resolved through Eureka.

### JWT Stateless Authentication
The Auth service issues signed JWTs. The Gateway validates tokens before routing, and propagates authorization via the `X-Auth-User-Role` header — no session state stored server-side.

### Gateway Secret
A shared secret validates that internal service-to-service calls originated from the Gateway, preventing direct unauthorized access to downstream services.

### Flyway Migrations
Database schema evolution managed by versioned SQL scripts under each service's `src/main/resources/db/migration/` directory.

### Multi-stage Docker Builds
Each service uses a multi-stage `Dockerfile` — first stage compiles with full JDK, second stage runs with minimal JRE for smaller images and faster startups.

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes with descriptive messages
4. Push to your branch (`git push origin feature/your-feature`)
5. Open a Pull Request against `main`

Please ensure:
- All existing tests pass (`./mvnw test` in each service)
- New features include appropriate test coverage
- Code follows existing project conventions

---

## License

This project is provided for educational and demonstration purposes.

---

Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team.
