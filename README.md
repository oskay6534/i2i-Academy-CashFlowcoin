# CryptoPal

CryptoPal is a full-stack cryptocurrency trading simulator. Users can register, receive a randomized virtual cash balance, monitor market prices, execute buy and sell orders, inspect portfolios and transaction history, and request Gemini-powered market insights.

## Architecture

- **Frontend:** React 19, TypeScript and Vite
- **Backend:** Java 17, Spring Boot, Spring Data JPA and Springdoc OpenAPI
- **PostgreSQL:** durable users, wallets, portfolios, trades and price history
- **Redis:** session tokens and latest cryptocurrency prices
- **Flyway:** versioned PostgreSQL schema migrations
- **Optional observability:** Elasticsearch and Kibana
- **Quality:** JUnit integration/unit tests and Selenium browser tests

## Repository layout

```text
backend/                 Spring Boot modular-monolith API
database/                Database implementation notes
docs/                    Deployment and Elastic Stack documentation
frontend/                React single-page application
infra/observability/     Elasticsearch and Kibana Docker Compose profile
tests/e2e/               Selenium browser-test Maven module
```

## Prerequisites

- Java 17+
- Node.js 20+
- Docker Desktop with at least 4 GB allocated memory
- A Google Gemini API key for the AI chat feature

## Environment configuration

Copy `.env.example` to `.env`. The `.env` file is ignored by Git and must never be committed.

```powershell
Copy-Item .env.example .env
```

Set a valid `GEMINI_API_KEY` in `.env`. The backend reads this file automatically when started from `backend/`; deployment platforms should supply the same values as environment variables.

## Run locally

### 1. Start PostgreSQL and Redis

```powershell
docker compose up -d
```

This starts PostgreSQL on `localhost:55432` and Redis on `localhost:6379`.

### 2. Start the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`.

### 3. Start the frontend

Open another terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`.

## Database migrations

The schema source of truth is [V1__initial_schema.sql](backend/src/main/resources/db/migration/V1__initial_schema.sql). Flyway applies it automatically and Hibernate validates the schema at startup.

For a completely fresh local database, remove only the local Docker volumes and recreate the services:

```powershell
docker compose down -v
docker compose up -d
```

This removes local PostgreSQL and Redis data.

## API documentation

With the backend running, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

## Tests

### Backend unit test

```powershell
cd backend
.\mvnw.cmd -Dtest=TickerEngineProviderTest test
```

### Backend integration tests

```powershell
cd backend
.\mvnw.cmd test
```

### Selenium end-to-end tests

Start the backend and frontend first. Then start Selenium Grid:

```powershell
docker run -d --rm --name cryptopal-selenium --add-host=host.docker.internal:host-gateway -p 4444:4444 selenium/standalone-chrome:latest
$env:APP_URL = 'http://host.docker.internal:5173'
$env:SELENIUM_GRID_URL = 'http://localhost:4444/wd/hub'
mvn -f tests/e2e/pom.xml test
```

## Elasticsearch and Kibana (optional)

See [docs/elasticsearch.md](docs/elasticsearch.md) for the observability profile, demo users, data indexing and Kibana dashboard setup.

## CI/CD

[Jenkinsfile](Jenkinsfile) runs Docker dependencies, backend tests, the frontend production build and optional Selenium tests. Render deployment metadata remains in [render.yaml](render.yaml).