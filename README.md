# Bank Card Management System

A production-ready REST API for managing bank cards, built with Spring Boot 3. Features JWT authentication, role-based access control (ADMIN/USER), AES-encrypted card numbers, fund transfers, filtering with pagination, Liquibase DB migrations, Swagger UI, and Docker Compose for local development.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (JJWT 0.11) |
| Persistence | Spring Data JPA + PostgreSQL |
| Migrations | Liquibase |
| Mapping | MapStruct |
| Documentation | SpringDoc OpenAPI / Swagger UI |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito + MockMvc |

---

## Getting Started

### Prerequisites

- Docker & Docker Compose installed
- Java 17+ (only needed if running without Docker)
- Maven 3.9+ (only needed if running without Docker)

### Run with Docker Compose (recommended)

```bash
git clone https://github.com/DemeedS/bank-card-system.git
cd bank-card-system
docker-compose up --build
```

The app will start at `http://localhost:8080`. PostgreSQL runs on port `5432`.

### Run locally without Docker

1. Start a PostgreSQL instance and create a database named `bankdb`
2. Copy and configure environment variables:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=bankdb
export DB_USER=bankuser
export DB_PASSWORD=bankpassword
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
export JWT_EXPIRATION=86400000
export CARD_ENCRYPTION_KEY=MySecretCardKey1
```

3. Build and run:

```bash
mvn clean package -DskipTests
java -jar target/card-management-1.0.0.jar
```

---

## API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

OpenAPI JSON spec: `http://localhost:8080/v3/api-docs`

---

## Default Admin Account

Created automatically via Liquibase migration:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Role | `ADMIN` |

> ⚠️ Change the admin password immediately in a production environment.

---

## Authentication Flow

1. **Register** a new user: `POST /api/v1/auth/register`
2. **Login** to receive a JWT: `POST /api/v1/auth/login`
3. **Include the token** in all subsequent requests:
   ```
   Authorization: Bearer <your-token>
   ```

---

## API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login and receive JWT |

### Cards (User)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/cards` | USER, ADMIN | Get my cards (filterable, paginated) |
| GET | `/api/v1/cards/{id}` | USER, ADMIN | Get my card by ID |
| POST | `/api/v1/cards/{id}/request-block` | USER, ADMIN | Request to block a card |

### Transfers
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/transfers` | USER, ADMIN | Transfer between own cards |

### Admin
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/admin/cards` | ADMIN | Create a card for a user |
| GET | `/api/v1/admin/cards` | ADMIN | Get all cards (filterable, paginated) |
| GET | `/api/v1/admin/cards/{id}` | ADMIN | Get any card by ID |
| PATCH | `/api/v1/admin/cards/{id}/status` | ADMIN | Set card status |
| DELETE | `/api/v1/admin/cards/{id}` | ADMIN | Delete a card |
| GET | `/api/v1/admin/users` | ADMIN | Get all users |
| GET | `/api/v1/admin/users/{id}` | ADMIN | Get user by ID |
| PATCH | `/api/v1/admin/users/{id}/enable` | ADMIN | Enable/disable user |
| DELETE | `/api/v1/admin/users/{id}` | ADMIN | Delete user |

### User Profile
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/users/me` | USER, ADMIN | Get current user profile |

---

## Card Security

- Card numbers are **AES-128 encrypted** before storage — the raw number never touches the database in plain text
- All API responses show only the **masked number**: `**** **** **** 1234`
- The encrypted value is never exposed in any response DTO

---

## Filtering & Pagination

Cards support filtering by status and full pagination control:

```
GET /api/v1/cards?status=ACTIVE&page=0&size=10&sortBy=createdAt&sortDir=desc
GET /api/v1/admin/cards?status=BLOCKED&page=1&size=5
```

---

## Running Tests

```bash
mvn test
```

Tests cover:
- `AuthService` — register, login, duplicate validation
- `CardService` — create, status changes, expiry logic, ownership checks
- `TransferService` — successful transfers, insufficient funds, inactive cards, same-card guard
- `CardController` — HTTP responses, error handling, filter params

---

## Project Structure

```
src/
├── main/
│   ├── java/com/bank/card/
│   │   ├── config/          # Security, OpenAPI, AES encryption
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Request & response DTOs
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Custom exceptions & global handler
│   │   ├── mapper/          # MapStruct mappers
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JWT filter, service, utils
│   │   └── service/         # Business logic interfaces & implementations
│   └── resources/
│       ├── application.yml
│       └── db/changelog/    # Liquibase migrations
└── test/
    └── java/com/bank/card/
        ├── controller/      # MockMvc controller tests
        └── service/         # Unit tests for services
```
