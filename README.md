# Nexus Bank — Card Management API

![CI](https://github.com/DemeedS/bank-card-system/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

A production-ready REST API for managing bank cards — built with Spring Boot 3, Spring Security, JWT authentication, AES-128 card encryption, and PostgreSQL. Deployed on Render with a Neon managed database.

**Live demo:** https://bank-card-system.onrender.com  
**API docs (Swagger UI):** https://bank-card-system.onrender.com/swagger-ui.html

---

## Features

- **JWT authentication** — stateless, 24-hour tokens with role-based access control (USER / ADMIN)
- **AES-128 card encryption** — raw card numbers are never stored in plain text; only masked values (`**** **** **** 1234`) are returned in responses
- **Fund transfers** — atomic transfers between cards with overdraft protection, balance validation, and card status checks
- **Admin controls** — create cards, block/unblock/delete cards, manage users, view all accounts
- **Filtering & pagination** — all list endpoints support status filtering, page/size/sort parameters
- **Liquibase migrations** — schema is version-controlled and applied automatically on startup
- **Swagger UI** — fully documented interactive API explorer

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (JJWT) |
| Persistence | Spring Data JPA + PostgreSQL |
| Migrations | Liquibase |
| Mapping | MapStruct |
| Documentation | SpringDoc OpenAPI 3 / Swagger UI |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + MockMvc |
| CI | GitHub Actions |
| Hosting | Render (app) + Neon (database) |

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21+ and Maven 3.9+ _(only needed without Docker)_

### Run with Docker Compose (recommended)

```bash
git clone https://github.com/DemeedS/bank-card-system.git
cd bank-card-system
cp .env.example .env      # fill in your values
docker-compose up --build
```

App starts at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Run locally without Docker

1. Start a PostgreSQL instance with a database named `bankdb`
2. Set environment variables:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=bankdb
export DB_USER=bankuser
export DB_PASSWORD=<your_db_password>
export JWT_SECRET=<64-char-hex-string>
export JWT_EXPIRATION=86400000
export CARD_ENCRYPTION_KEY=<exactly-16-chars>
```

> `JWT_SECRET` — generate a secure 64-character hex string (e.g. `openssl rand -hex 32`).  
> `CARD_ENCRYPTION_KEY` — must be exactly 16 characters (AES-128).

3. Build and run:

```bash
mvn clean package -DskipTests
java -jar target/card-management-1.0.0.jar
```

---

## Default Admin Account

Created automatically via Liquibase on first startup:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Role | `ADMIN` |

> Change the admin password immediately in any production environment.

---

## Authentication

All protected endpoints require a Bearer token in the `Authorization` header.

**Step 1 — Register:**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "alice123",
  "email": "alice@example.com"
}
```

**Step 2 — Login:**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "alice123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "alice",
  "role": "USER",
  "expiresIn": 86400000
}
```

**Step 3 — Use the token:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## API Reference

### Auth — Public

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login and receive JWT |

### Cards — USER + ADMIN

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/cards` | List own cards (filterable, paginated) |
| `GET` | `/api/v1/cards/{id}` | Get own card by ID |
| `POST` | `/api/v1/cards/{id}/request-block` | Request to block a card |

**Query parameters for card listing:**
```
GET /api/v1/cards?status=ACTIVE&page=0&size=10&sortBy=createdAt&sortDir=desc
```

| Param | Values | Default |
|---|---|---|
| `status` | `ACTIVE`, `BLOCKED`, `EXPIRED` | all |
| `page` | integer | `0` |
| `size` | integer | `10` |
| `sortBy` | `createdAt`, `balance`, `expiryDate` | `createdAt` |
| `sortDir` | `asc`, `desc` | `desc` |

### Transfers — USER + ADMIN

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/transfers` | Transfer funds between own cards |

```http
POST /api/v1/transfers
Authorization: Bearer <token>
Content-Type: application/json

{
  "fromCardId": 1,
  "toCardId": 2,
  "amount": 250.00
}
```

### Admin — ADMIN only

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/admin/cards` | Create a card for any user |
| `GET` | `/api/v1/admin/cards` | List all cards (filterable, paginated) |
| `GET` | `/api/v1/admin/cards/{id}` | Get any card by ID |
| `PATCH` | `/api/v1/admin/cards/{id}/status?status=BLOCKED` | Set card status |
| `DELETE` | `/api/v1/admin/cards/{id}` | Delete a card permanently |
| `GET` | `/api/v1/admin/users` | List all users |
| `GET` | `/api/v1/admin/users/{id}` | Get user by ID |
| `PATCH` | `/api/v1/admin/users/{id}/enable?enabled=false` | Enable / disable a user |
| `DELETE` | `/api/v1/admin/users/{id}` | Delete a user and all their cards |

### User Profile

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/users/me` | Get current user profile |
| `PUT` | `/api/v1/users/me` | Update current user profile |

---

## Card Security

- Card numbers are **AES-128 encrypted** before storage — the raw number never touches the database in plain text
- Responses always return only the **masked number**: `**** **** **** 1234`
- The encrypted field is never serialized in any response DTO

---

## Running Tests

```bash
mvn test
```

Test coverage includes:

| Class | What's tested |
|---|---|
| `AuthService` | Register, login, duplicate username/email validation |
| `CardService` | Create, status changes, expiry logic, ownership enforcement |
| `TransferService` | Valid transfers, insufficient funds, inactive card guard, same-card guard |
| `CardController` | HTTP responses, error handling, pagination/filter params |
| `AdminController` | Admin-only access, user/card management |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/bank/card/
│   │   ├── config/          # SecurityConfig, CardEncryptionService, OpenApiConfig
│   │   ├── controller/      # REST controllers (Auth, Card, Admin, Transfer, User)
│   │   ├── dto/             # Request & response DTOs
│   │   ├── entity/          # JPA entities (User, Card) + enums (Role, CardStatus)
│   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   ├── mapper/          # MapStruct mappers (CardMapper)
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
│   │   └── service/         # Business logic — interfaces + Impl classes
│   └── resources/
│       ├── application.yml
│       ├── static/          # Landing page + Swagger custom theme
│       └── db/changelog/    # Liquibase migration files
└── test/
    └── java/com/bank/card/
        ├── controller/      # MockMvc controller tests
        └── service/         # Unit tests with Mockito
```

---

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `bankdb` |
| `DB_USER` | Database username | `bankuser` |
| `DB_PASSWORD` | Database password | — |
| `JWT_SECRET` | 64-char hex string for signing tokens | — |
| `JWT_EXPIRATION` | Token TTL in milliseconds | `86400000` |
| `CARD_ENCRYPTION_KEY` | AES-128 key — must be exactly 16 characters | — |
