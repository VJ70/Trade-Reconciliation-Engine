-- V1__init_schema.sql
-- Trade Reconciliation Engine - Initial Schema
-- Flyway migration: runs once on first startup

-- ── BROKERS ──────────────────────────────────────────────────────────────────
CREATE TABLE brokers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(10)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── USERS ─────────────────────────────────────────────────────────────────────
CREATE TYPE user_role AS ENUM ('TRADER', 'ADMIN');

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL DEFAULT 'TRADER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── TRADES ────────────────────────────────────────────────────────────────────
CREATE TYPE trade_side   AS ENUM ('BUY', 'SELL');
CREATE TYPE trade_source AS ENUM ('INTERNAL', 'BROKER');
CREATE TYPE trade_status AS ENUM ('PENDING', 'MATCHED', 'BREAK');

CREATE TABLE trades (
    id         BIGSERIAL    PRIMARY KEY,
    symbol     VARCHAR(10)  NOT NULL,
    quantity   DECIMAL(18,6) NOT NULL,
    price      DECIMAL(18,6) NOT NULL,
    side       trade_side   NOT NULL,
    source     trade_source NOT NULL,
    broker_id  BIGINT       REFERENCES brokers(id),
    status     trade_status NOT NULL DEFAULT 'PENDING',
    trade_ref  VARCHAR(50),
    timestamp  TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Indexes: chosen to speed up reconciliation JOIN queries
CREATE INDEX idx_trades_symbol          ON trades(symbol);
CREATE INDEX idx_trades_timestamp       ON trades(timestamp);
CREATE INDEX idx_trades_source_status   ON trades(source, status);
CREATE INDEX idx_trades_broker_status   ON trades(broker_id, status);
CREATE INDEX idx_trades_symbol_ts       ON trades(symbol, timestamp);

-- ── RECONCILIATION BREAKS ────────────────────────────────────────────────────
CREATE TYPE break_type     AS ENUM ('QUANTITY_MISMATCH', 'PRICE_MISMATCH', 'UNMATCHED');
CREATE TYPE break_severity AS ENUM ('HIGH', 'LOW');

CREATE TABLE reconciliation_breaks (
    id                 BIGSERIAL       PRIMARY KEY,
    internal_trade_id  BIGINT          REFERENCES trades(id),
    broker_trade_id    BIGINT          REFERENCES trades(id),
    break_type         break_type      NOT NULL,
    severity           break_severity  NOT NULL,
    expected_value     DECIMAL(18,6),
    actual_value       DECIMAL(18,6),
    resolved_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_breaks_internal_trade ON reconciliation_breaks(internal_trade_id);
CREATE INDEX idx_breaks_resolved       ON reconciliation_breaks(resolved_at);

-- ── PNL SUMMARY ───────────────────────────────────────────────────────────────
CREATE TABLE pnl_summary (
    id              BIGSERIAL    PRIMARY KEY,
    symbol          VARCHAR(10)  NOT NULL,
    date            DATE         NOT NULL,
    realized_pnl    DECIMAL(18,6) NOT NULL DEFAULT 0,
    trade_count     INT          NOT NULL DEFAULT 0,
    calculated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(symbol, date)
);

CREATE INDEX idx_pnl_date ON pnl_summary(date);
