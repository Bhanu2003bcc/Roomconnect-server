-- Performance composite indexes for platform stats queries
-- Enables index-only scans for COUNT(role, phone_verified) and COUNT(status)
CREATE INDEX IF NOT EXISTS idx_users_role_phone_verified ON users (role, phone_verified);
CREATE INDEX IF NOT EXISTS idx_listings_status ON listings (status);
