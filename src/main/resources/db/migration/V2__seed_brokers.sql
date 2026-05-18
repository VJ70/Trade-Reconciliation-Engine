-- V2__seed_brokers.sql
-- Seed 3 brokers used by DataSeeder and tests

INSERT INTO brokers (name, code) VALUES
    ('Goldman Sachs', 'GS'),
    ('Morgan Stanley', 'MS'),
    ('JP Morgan',      'JPM')
ON CONFLICT (code) DO NOTHING;
