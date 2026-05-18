# Trade Reconciliation Engine

A production-shaped post-trade reconciliation system built with **Java 17 + Spring Boot 3.2 + PostgreSQL + ElasticSearch + JWT auth**.

Built to demonstrate backend engineering depth for quantitative trading firms — reconciliation, trade capture, P&L, break management, role-based access, and full-text search.

---

## Architecture

```
Client (Postman / Swagger UI)
        │  HTTP + Bearer JWT
        ▼
┌─────────────────────────────────┐
│  JwtAuthenticationFilter        │  ← validates token on every request
│  Spring SecurityFilterChain     │  ← role check (@PreAuthorize)
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  Controllers                    │  AuthController · TradeController
│  (no business logic)            │  ReconciliationController · BreakController · PnlController
└────────────┬────────────────────┘
             │  DTOs in / DTOs out
┌────────────▼────────────────────┐
│  Services (business logic)      │  ReconciliationService · TradeService
│                                 │  AuthService · PnlService · BreakService
└────────────┬────────────────────┘
             │
┌────────────▼────────────────────┐
│  Repositories (JPA)             │  TradeRepository · BreakRepository
│                                 │  PnlRepository · UserRepository
└────────────┬────────────────────┘
             │
┌────────────▼────────┐  ┌────────────────────┐
│  PostgreSQL 15      │  │  ElasticSearch 8   │
│  (primary store)    │  │  (search index)    │
└─────────────────────┘  └────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6 + JWT (jjwt 0.12, HmacSHA256) |
| ORM | Spring Data JPA + Hibernate |
| Primary DB | PostgreSQL 15 |
| Search | ElasticSearch 8 |
| Migrations | Flyway |
| Docs | Springdoc OpenAPI (Swagger UI) |
| Tests | JUnit 5 + Mockito |
| Container | Docker + Docker Compose |
| Build | Maven |

---

## Quick Start (Docker — recommended)

### Prerequisites
- Docker Desktop installed and running
- Ports 8080, 5432, 9200 free

```bash
# 1. Clone
git clone https://github.com/VJ70/trade-reconciliation-engine.git
cd trade-reconciliation-engine

# 2. Start all three services (app + postgres + elasticsearch)
docker-compose up --build

# 3. Wait for this log line:
#    Started TradeReconciliationApplication in X.XXX seconds

# 4. Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

On first boot, the seeder creates:
- **1000 trades** (500 internal + 500 broker, ~15% with mismatches)
- **2 users**: `admin / admin123` (ADMIN role) and `trader / trader123` (TRADER role)

---

## Manual Setup (without Docker)

### 1. Install prerequisites

```bash
# Java 17
java -version   # must show 17.x

# Maven
mvn -version

# PostgreSQL (local)
brew install postgresql@15   # macOS
# or: sudo apt install postgresql-15  (Ubuntu)

# ElasticSearch 8
brew install elastic/tap/elasticsearch-full  # macOS
# or download from https://www.elastic.co/downloads/elasticsearch
```

### 2. Create PostgreSQL database

```sql
-- Connect as postgres superuser
psql -U postgres

CREATE DATABASE trade_recon;
\q
```

### 3. Configure environment

```bash
cp .env.example .env
# Edit .env with your DB password if different from 'postgres'
```

### 4. Start ElasticSearch

```bash
elasticsearch   # starts on http://localhost:9200

# Verify:
curl http://localhost:9200/_cluster/health
# Expected: {"status":"green",...} or "yellow" (both fine for local)
```

### 5. Run the app

```bash
mvn spring-boot:run
```

---

## Verifying Each Stack Layer

Work through these in order — each step confirms one layer before moving to the next.

---

### Layer 1 — PostgreSQL connection

**What to check:** Flyway ran migrations, tables exist, data is seeded.

```bash
# Connect to the DB
psql -U postgres -d trade_recon

# Check tables were created by Flyway
\dt
# Expected: brokers, flyway_schema_history, pnl_summary,
#           reconciliation_breaks, trades, users

# Check Flyway ran cleanly
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
# Expected: V1 and V2 both show success = true

# Check seeded data
SELECT COUNT(*) FROM trades;    -- 1000
SELECT COUNT(*) FROM users;     -- 2
SELECT name, code FROM brokers; -- Goldman Sachs, Morgan Stanley, JP Morgan

# Check indexes exist
\d trades
# Look for: idx_trades_symbol, idx_trades_timestamp, idx_trades_source_status

\q
```

**In app logs, look for:**
```
Successfully applied 2 migrations to schema "public"
Seeded 1000 trades (500 internal + 500 broker, ~15% with mismatches)
```

**If this fails:** Check `application.yml` DB credentials match your local Postgres.

---

### Layer 2 — Spring Boot startup

**What to check:** App boots without errors, all beans wired correctly.

```bash
# App log should end with:
# Started TradeReconciliationApplication in X.XXX seconds (JVM running for X.XXX)

# If you see BeanCreationException → a @Component or @Service failed to wire
# Common cause: DB not reachable. Check postgres is running:
pg_ctl status   # or: brew services list | grep postgresql
```

**Verify health endpoint (no auth required):**
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

### Layer 3 — Security layer (JWT)

**What to check:** Unauthenticated requests are rejected, login returns a token.

```bash
# Should return 403 without token
curl -i http://localhost:8080/api/trades
# Expected: HTTP/1.1 403

# Register a new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test1234","role":"TRADER"}'
# Expected: {"token":"eyJ...","username":"testuser","role":"TRADER","expiresIn":86400000}

# Login with seeded admin user
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# Copy the token from the response — you'll use it in every request below
export ADMIN_TOKEN="eyJ..."

# Login as trader
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"trader","password":"trader123"}'
export TRADER_TOKEN="eyJ..."
```

**Decode your token (optional — understand the structure):**
```bash
# Paste your token at https://jwt.io
# Header:  {"alg":"HS256","typ":"JWT"}
# Payload: {"sub":"admin","role":"ROLE_ADMIN","iat":...,"exp":...}
# Signature: HmacSHA256(base64(header)+"."+base64(payload), secretKey)
```

---

### Layer 4 — Controller + Service layer (Trade ingestion)

**What to check:** Trade endpoints accept requests, validate input, return correct responses.

```bash
# Ingest an internal trade (TRADER or ADMIN)
curl -X POST http://localhost:8080/api/trades \
  -H "Authorization: Bearer $TRADER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "quantity": 100,
    "price": 175.50,
    "side": "BUY",
    "source": "INTERNAL",
    "brokerId": 1,
    "tradeRef": "TEST-001",
    "timestamp": "2024-01-15T10:00:00Z"
  }'
# Expected: 201 Created with trade JSON including id and status: PENDING

# Ingest matching broker trade
curl -X POST http://localhost:8080/api/trades \
  -H "Authorization: Bearer $TRADER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "quantity": 100,
    "price": 175.50,
    "side": "BUY",
    "source": "BROKER",
    "brokerId": 1,
    "tradeRef": "BRK-TEST-001",
    "timestamp": "2024-01-15T10:01:00Z"
  }'

# List trades (paginated)
curl "http://localhost:8080/api/trades?page=0&size=10" \
  -H "Authorization: Bearer $TRADER_TOKEN"
# Expected: page object with content array, totalElements: 1002 (1000 seeded + 2 you just added)

# Validation test — should return 400 with field errors
curl -X POST http://localhost:8080/api/trades \
  -H "Authorization: Bearer $TRADER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "", "quantity": -5}'
# Expected: 400 with {"error":"Validation Failed","fields":{"symbol":"...","price":"...",...}}

# Role test — TRADER cannot trigger reconciliation
curl -X POST http://localhost:8080/api/reconcile/run \
  -H "Authorization: Bearer $TRADER_TOKEN"
# Expected: 403 Forbidden
```

---

### Layer 5 — Reconciliation engine

**What to check:** The core algorithm runs, matches trades, creates breaks.

```bash
# Trigger reconciliation (ADMIN only)
curl -X POST http://localhost:8080/api/reconcile/run \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Expected response (numbers will vary slightly):
# {
#   "totalProcessed": 501,
#   "matched": ~425,
#   "breaksCreated": ~55,
#   "unmatched": ~20,
#   "durationMs": ~150,
#   "runAt": "2024-..."
# }

# Check run status
curl http://localhost:8080/api/reconcile/status \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# List all breaks
curl "http://localhost:8080/api/breaks" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# List only unresolved breaks
curl "http://localhost:8080/api/breaks?unresolvedOnly=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Resolve a break (use an id from the list above)
curl -X PATCH http://localhost:8080/api/breaks/1/resolve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: break object with resolved: true and resolvedAt timestamp

# Verify in DB
psql -U postgres -d trade_recon -c "
  SELECT break_type, severity, COUNT(*) 
  FROM reconciliation_breaks 
  GROUP BY break_type, severity 
  ORDER BY severity, break_type;"
# Expected: rows showing QUANTITY_MISMATCH, PRICE_MISMATCH, UNMATCHED splits
```

---

### Layer 6 — P&L calculation

```bash
# Trigger P&L calculation for a date (must have matched trades for that date)
curl -X POST "http://localhost:8080/api/pnl/calculate?date=2024-01-15" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Retrieve P&L for a date
curl "http://localhost:8080/api/pnl?date=2024-01-15" \
  -H "Authorization: Bearer $TRADER_TOKEN"
# Expected: array of {symbol, date, realizedPnl, tradeCount}
```

---

### Layer 7 — ElasticSearch

**What to check:** ES is reachable, trades are indexed, search returns results.

```bash
# Check ES cluster health directly
curl http://localhost:9200/_cluster/health?pretty
# Expected: "status":"green" or "yellow" — both fine

# Check trades index exists (after at least one trade is ingested)
curl http://localhost:9200/trades/_count
# Expected: {"count": 1000, ...} (or however many trades are ingested)

# Search via the API
curl "http://localhost:8080/api/trades/search?q=AAPL" \
  -H "Authorization: Bearer $TRADER_TOKEN"
# Expected: array of TradeDocuments with symbol: "AAPL"

# Raw ES query (useful for debugging)
curl "http://localhost:9200/trades/_search?q=symbol:AAPL&size=5&pretty"
```

**If ES is unreachable:** The app continues working — ES failures are caught and logged,
not propagated. Check docker-compose logs: `docker-compose logs elasticsearch`

---

### Layer 8 — Swagger UI (full end-to-end)

Open **http://localhost:8080/swagger-ui.html**

1. Click **POST /auth/login** → Try it out → enter `admin` / `admin123` → Execute
2. Copy the `token` from the response
3. Click **Authorize** (top right) → paste the token → Authorize
4. Now all endpoints are unlocked — test them all from the browser

---

### Layer 9 — Unit tests

```bash
# Run all tests
mvn test

# Expected output:
# Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS

# Run a specific test class
mvn test -Dtest=ReconciliationServiceTest

# What each test covers:
# ReconciliationServiceTest:
#   - cleanMatch_shouldMarkBothMatched
#   - quantityMismatch_shouldCreateHighSeverityBreak
#   - noBrokerMatch_shouldCreateUnmatchedBreak
#   - emptyQueue_shouldReturnZeroCounts
# AuthServiceTest:
#   - register_newUser_returnsToken
#   - register_duplicate_throwsException
```

---

## Database Queries Worth Understanding

These are the kinds of queries asked in TRC interviews:

```sql
-- Reconciliation JOIN: find broker trades within 5-minute window of internal trade
SELECT b.id, b.quantity, b.price
FROM trades i
JOIN trades b ON i.symbol = b.symbol AND i.side = b.side
WHERE i.source = 'INTERNAL' AND b.source = 'BROKER'
  AND i.status = 'PENDING'  AND b.status = 'PENDING'
  AND b.timestamp BETWEEN i.timestamp - INTERVAL '5 minutes'
                      AND i.timestamp + INTERVAL '5 minutes';

-- Break summary by type and severity
SELECT break_type, severity, COUNT(*), AVG(ABS(expected_value - actual_value)) as avg_delta
FROM reconciliation_breaks
WHERE resolved_at IS NULL
GROUP BY break_type, severity
ORDER BY severity, break_type;

-- Daily P&L per symbol using window functions
SELECT symbol,
       SUM(CASE WHEN side = 'SELL' THEN price * quantity ELSE -(price * quantity) END) as realized_pnl,
       COUNT(*) as trade_count
FROM trades
WHERE status = 'MATCHED' AND source = 'INTERNAL'
  AND DATE(timestamp) = '2024-01-15'
GROUP BY symbol
ORDER BY realized_pnl DESC;

-- Trade volume by broker (shows JOIN + GROUP BY)
SELECT b.name, COUNT(t.id) as trade_count, SUM(t.quantity * t.price) as notional
FROM trades t
JOIN brokers b ON t.broker_id = b.id
GROUP BY b.name
ORDER BY notional DESC;
```

---

## Scaling This System (interview answer)

When asked "how would you scale this to 1M trades/day?":

1. **Shard `trades` by `broker_id`** — horizontal partitioning keeps each shard manageable
2. **Read replicas** for P&L aggregation and break queries — isolates heavy reads from write path
3. **Message queue (Kafka)** between trade ingestion and reconciliation — decouple and buffer spikes
4. **Async reconciliation** — run as a scheduled job or event-driven, not synchronous HTTP
5. **Redis cache** for P&L summaries — cache per symbol/date, invalidate on new matched trade
6. **Connection pooling** (HikariCP — already included in Spring Boot) — tune pool size

---

## Project Structure

```
src/main/java/com/tradereconciliation/
├── controller/          REST layer — no business logic
├── service/             All business logic
├── repository/          JPA interfaces + custom @Query
├── model/               @Entity classes
├── dto/
│   ├── request/         Input validation objects
│   └── response/        Outbound response objects (entities never exposed directly)
├── security/            JwtService · JwtAuthenticationFilter
├── elasticsearch/       TradeDocument · TradeSearchRepository
├── exception/           GlobalExceptionHandler · custom exceptions
├── config/              SecurityConfig · SwaggerConfig
└── seeder/              DataSeeder (1000 trades on startup)

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__init_schema.sql
    └── V2__seed_brokers.sql
```

---

## Default Credentials

| User | Password | Role | Can access |
|---|---|---|---|
| admin | admin123 | ADMIN | All endpoints |
| trader | trader123 | TRADER | Trade ingestion, P&L read, ES search |

---

## Resume Bullets

> Built a trade reconciliation engine in **Java (Spring Boot 3.2)** that ingests, matches, and flags breaks across broker and internal trade records using **PostgreSQL with indexed schemas** and **Flyway migrations**

> Designed **13 REST APIs** for trade capture, break management, and daily P&L aggregation with **role-based JWT authentication** (HmacSHA256); secured with Spring Security 6

> Integrated **ElasticSearch** for sub-100ms trade search across 1000+ records; containerized full stack with **Docker Compose** (app + PostgreSQL + ElasticSearch)
