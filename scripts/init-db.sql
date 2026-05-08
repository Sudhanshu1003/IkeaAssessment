-- Initialize database for warehouse application
-- This script runs when PostgreSQL container starts

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create database schema (if not using Hibernate auto-creation)
-- Note: With Quarkus/Hibernate, tables are created automatically

-- Insert initial data for testing
INSERT INTO warehouse (id, business_unit_code, location, capacity, stock, created_at, version, archived_at) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'WH-001', 'AMSTERDAM-001', 40, 25, CURRENT_TIMESTAMP, 1, NULL),
('550e8400-e29b-41d4-a716-446655440002', 'WH-002', 'ZWOLLE-001', 35, 20, CURRENT_TIMESTAMP, 1, NULL),
('550e8400-e29b-41d4-a716-446655440003', 'WH-003', 'TILBURG-001', 30, 15, CURRENT_TIMESTAMP, 1, NULL),
('550e8400-e29b-41d4-a716-446655440004', 'WH-004', 'AMSTERDAM-001', 25, 10, CURRENT_TIMESTAMP, 1, NULL),
('550e8400-e29b-41d4-a716-446655440005', 'WH-005', 'ZWOLLE-001', 45, 30, CURRENT_TIMESTAMP, 1, NULL)
ON CONFLICT (id) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_warehouse_business_unit_code ON warehouse(business_unit_code);
CREATE INDEX IF NOT EXISTS idx_warehouse_location ON warehouse(location);
CREATE INDEX IF NOT EXISTS idx_warehouse_capacity ON warehouse(capacity);
CREATE INDEX IF NOT EXISTS idx_warehouse_stock ON warehouse(stock);
CREATE INDEX IF NOT EXISTS idx_warehouse_archived_at ON warehouse(archived_at);

-- Grant permissions (if needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO warehouse;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO warehouse;
