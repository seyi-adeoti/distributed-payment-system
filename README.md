# Distributed Payment Platform

A highly scalable, resilient, and observable distributed payment platform built with Spring Boot microservices, Kafka for event-driven messaging, and a robust Saga pattern for distributed transactions.

## 🏗 Architecture Overview

The system is composed of several independent microservices communicating dynamically via Spring Cloud Eureka and Kafka.

### Core Microservices

1. **User Service** (`:8081`): 
   - Manages user accounts and authentication.
   - Issues JWT tokens for authenticated sessions.
   - Triggers `UserCreatedEvent` on the `user.events` Kafka topic.

2. **Wallet Service** (`:8082`): 
   - Manages digital wallets and balances.
   - Integrates mock AML (Anti-Money Laundering) checks protected by **Resilience4j Circuit Breakers**.
   - Handles limits based on KYC Tiers.

3. **Payment Service** (`:8083`): 
   - Orchestrates the payment flows and distributed transactions using the **Saga Pattern**.
   - Employs the **Outbox Pattern** to reliably publish events to Kafka.
   - Manages Dead Letter Queues (DLQ) for failed payment events.

4. **Ledger Service** (`:8085`): 
   - Maintains double-entry bookkeeping records for compliance.
   - Acts as a Saga Participant listening to `payment.events`.

5. **Notification Service** (`:8087`): 
   - Listens to Kafka topics to send out asynchronous alerts (Welcome Emails, Transfer Receipts).
   - Features a graceful fallback mechanism via Resilience4j if the User Service is unreachable.

### Infrastructure & Gateway

* **Eureka Server** (`:8761`): Centralized Service Discovery and Registry.
* **API Gateway** (`:8080`): Reactive gateway handling all external client traffic.
  * **Dynamic Routing:** Routes requests using `lb://` (Eureka Load Balancer).
  * **Security:** Centralized JWT verification using `AuthFilter`.
  * **Rate Limiting:** Redis-backed rate limiter on authentication endpoints to prevent brute-force attacks.

## 🚀 Key Features

- **Distributed Transactions:** Reliable state transitions across services using Kafka-driven Sagas and Outbox patterns.
- **Resiliency:** Circuit breakers implemented via **Resilience4j** to gracefully handle downstream failures.
- **Observability:** Distributed tracing enabled out-of-the-box using **Micrometer** and **Zipkin**. Every request flow can be visually traced across the entire cluster via unique Trace IDs.
- **Security:** Stateless JWT authentication intercepted at the API Gateway.
- **Automated Database Migrations:** Flyway tracks and applies schema changes to PostgreSQL on startup.

## 🛠 Tech Stack

* **Java 17** & **Spring Boot 3.2.x**
* **Spring Cloud** (Gateway, Eureka Client/Server, Circuit Breaker)
* **Apache Kafka** (Event-Driven Architecture)
* **PostgreSQL** (Relational Database)
* **Redis** (Rate Limiting)
* **Zipkin & Micrometer** (Distributed Tracing)
* **Resilience4j** (Circuit Breakers)
* **Flyway** (Database Migrations)
* **Maven** (Build Tool)

## 📦 Getting Started

### Prerequisites
* Java 17
* Docker & Docker Compose
* Maven

### 1. Start Infrastructure Dependencies
Spin up the required infrastructure (Kafka, PostgreSQL, Redis, Zipkin) using Docker Compose:
```bash
docker-compose up -d
```

### 2. Build the Project
Compile and build all modules using the included Maven wrapper:
```bash
mvn clean install -DskipTests
```

### 3. Run the Services
It is recommended to run the services in the following order:

1. **Eureka Server** (`eureka-server`)
2. **API Gateway** (`api-gateway`)
3. **Core Services:** `user-service`, `wallet-service`, `payment-service`, `ledger-service`, `notification-service`

*(You can run them via your IDE or by executing `mvn spring-boot:run` in each directory).*

## 📊 Endpoints & Testing

- **API Gateway Base URL:** `http://localhost:8080`
- **Zipkin Dashboard:** `http://localhost:9411`
- **Eureka Dashboard:** `http://localhost:8761`

All external requests should go through the API Gateway. The Gateway will dynamically route `/api/v1/auth/**` to the User Service and `/api/v1/wallet/**` to the Wallet Service, etc., while validating JWTs and enforcing rate limits.

---
*Architected and developed with modern distributed system principles.*
