-- =============================================================================
-- Enterprise Field Force Management & CRM Database Schema
-- Architecture: PostgreSQL / MySQL Relational Schema with RBAC & Row Security
-- Entities: users, teams, areas, merchants, visits, attendance, live_locations, audit_logs
-- =============================================================================

-- 1. ENUMS / ROLES
-- Role Hierarchy: BDO (Level 1) -> TL (Level 2) -> ASM (Level 3) -> ADMIN (Level 4) -> MASTER (Level 5)

-- 2. AREAS (ASM Level)
CREATE TABLE IF NOT EXISTS areas (
    area_id VARCHAR(50) PRIMARY KEY,
    area_name VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    asm_uid VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. TEAMS (TL Level)
CREATE TABLE IF NOT EXISTS teams (
    team_id VARCHAR(50) PRIMARY KEY,
    team_name VARCHAR(100) NOT NULL,
    area_id VARCHAR(50) NOT NULL REFERENCES areas(area_id) ON DELETE CASCADE,
    tl_uid VARCHAR(50),
    target_visits_monthly INT DEFAULT 1200,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. USERS (BDO, TL, ASM, ADMIN, MASTER)
CREATE TABLE IF NOT EXISTS users (
    uid VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('BDO', 'TL', 'ASM', 'ADMIN', 'MASTER')),
    team_id VARCHAR(50) REFERENCES teams(team_id) ON DELETE SET NULL,
    area_id VARCHAR(50) REFERENCES areas(area_id) ON DELETE SET NULL,
    region VARCHAR(100) NOT NULL DEFAULT 'Main Region',
    phone VARCHAR(30),
    assigned_targets INT DEFAULT 50,
    status VARCHAR(20) DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive', 'Suspended')),
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraints back to users
ALTER TABLE areas ADD CONSTRAINT fk_areas_asm FOREIGN KEY (asm_uid) REFERENCES users(uid) ON DELETE SET NULL;
ALTER TABLE teams ADD CONSTRAINT fk_teams_tl FOREIGN KEY (tl_uid) REFERENCES users(uid) ON DELETE SET NULL;

-- 5. MERCHANTS (Expanded schema with full address fields)
CREATE TABLE IF NOT EXISTS merchants (
    merchant_id VARCHAR(50) PRIMARY KEY,
    merchant_name VARCHAR(150) NOT NULL,
    store_type VARCHAR(100) NOT NULL,
    merchant_category VARCHAR(100) DEFAULT 'General Retail',
    qr_code_id VARCHAR(100) UNIQUE,
    bdo_uid VARCHAR(50) REFERENCES users(uid) ON DELETE SET NULL,
    team_id VARCHAR(50) REFERENCES teams(team_id) ON DELETE SET NULL,
    area_id VARCHAR(50) REFERENCES areas(area_id) ON DELETE SET NULL,
    contact_person VARCHAR(150),
    mobile_number VARCHAR(30),
    street_address TEXT NOT NULL,
    landmark VARCHAR(150),
    area VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    operational_notes TEXT,
    status VARCHAR(20) DEFAULT 'Pending' CHECK (status IN ('Pending', 'Active', 'Rejected', 'Suspended')),
    verification_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. VISITS (BDO Visit Dispatches with image attachments & TL verification)
CREATE TABLE IF NOT EXISTS visits (
    id VARCHAR(50) PRIMARY KEY,
    visit_id VARCHAR(50) UNIQUE NOT NULL,
    bdo_uid VARCHAR(50) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    merchant_id VARCHAR(50) NOT NULL REFERENCES merchants(merchant_id) ON DELETE CASCADE,
    merchant_name VARCHAR(150) NOT NULL,
    team_id VARCHAR(50) REFERENCES teams(team_id) ON DELETE SET NULL,
    area_id VARCHAR(50) REFERENCES areas(area_id) ON DELETE SET NULL,
    timestamp BIGINT NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    purpose VARCHAR(100) DEFAULT 'QR Deployment' CHECK (purpose IN ('Onboarding', 'QR Deployment', 'Merchant Support', 'Stock Audit', 'Payment Dispute', 'Revisit')),
    notes TEXT,
    photo_url TEXT,
    photo_urls JSONB DEFAULT '[]'::jsonb,
    validation_status VARCHAR(20) DEFAULT 'PENDING' CHECK (validation_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    validated_by VARCHAR(50) REFERENCES users(uid) ON DELETE SET NULL,
    validated_at TIMESTAMP WITH TIME ZONE,
    feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. ATTENDANCE (Shift tracking & working hours calculation)
CREATE TABLE IF NOT EXISTS attendance (
    id VARCHAR(50) PRIMARY KEY,
    user_uid VARCHAR(50) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    user_name VARCHAR(150) NOT NULL,
    role VARCHAR(20) NOT NULL,
    team_id VARCHAR(50) REFERENCES teams(team_id) ON DELETE SET NULL,
    area_id VARCHAR(50) REFERENCES areas(area_id) ON DELETE SET NULL,
    date DATE NOT NULL,
    login_time TIMESTAMP WITH TIME ZONE NOT NULL,
    logout_time TIMESTAMP WITH TIME ZONE,
    total_working_hours DECIMAL(5, 2) DEFAULT 0.00,
    active_status VARCHAR(20) DEFAULT 'ONLINE' CHECK (active_status IN ('ONLINE', 'OFFLINE', 'ON_DUTY', 'ON_BREAK')),
    visits_conducted INT DEFAULT 0,
    last_ping TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_user_date UNIQUE (user_uid, date)
);

-- 8. LIVE_LOCATIONS (Real-time GPS telemetry stream)
CREATE TABLE IF NOT EXISTS live_locations (
    id VARCHAR(50) PRIMARY KEY,
    bdo_uid VARCHAR(50) NOT NULL REFERENCES users(uid) ON DELETE CASCADE,
    user_name VARCHAR(150) NOT NULL,
    team_id VARCHAR(50) REFERENCES teams(team_id) ON DELETE SET NULL,
    area_id VARCHAR(50) REFERENCES areas(area_id) ON DELETE SET NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    accuracy DECIMAL(6, 2) DEFAULT 5.0,
    speed DECIMAL(6, 2) DEFAULT 0.0,
    battery_level INT CHECK (battery_level BETWEEN 0 AND 100),
    timestamp BIGINT NOT NULL,
    is_online BOOLEAN DEFAULT TRUE,
    last_active_ping TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. AUDIT_LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_uid VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(50),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- PERFORMANCE INDEXES
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_users_role_team ON users(role, team_id);
CREATE INDEX IF NOT EXISTS idx_users_area ON users(area_id);
CREATE INDEX IF NOT EXISTS idx_merchants_bdo ON merchants(bdo_uid);
CREATE INDEX IF NOT EXISTS idx_merchants_status ON merchants(status);
CREATE INDEX IF NOT EXISTS idx_visits_bdo ON visits(bdo_uid);
CREATE INDEX IF NOT EXISTS idx_visits_team ON visits(team_id);
CREATE INDEX IF NOT EXISTS idx_visits_validation ON visits(validation_status);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(date, active_status);
CREATE INDEX IF NOT EXISTS idx_live_locations_active ON live_locations(bdo_uid, timestamp DESC);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES FOR HIERARCHICAL RBAC
-- =============================================================================
ALTER TABLE visits ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE live_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;

-- BDO Policy: Can only view and edit their own visits
CREATE POLICY bdo_visits_policy ON visits
    FOR ALL
    USING (
        current_setting('app.current_user_role', true) = 'BDO' 
        AND bdo_uid = current_setting('app.current_user_uid', true)
    );

-- TL Policy: Can view all visits in their team
CREATE POLICY tl_visits_policy ON visits
    FOR ALL
    USING (
        current_setting('app.current_user_role', true) = 'TL' 
        AND team_id = current_setting('app.current_user_team_id', true)
    );

-- ASM Policy: Can view all visits in their area
CREATE POLICY asm_visits_policy ON visits
    FOR ALL
    USING (
        current_setting('app.current_user_role', true) = 'ASM' 
        AND area_id = current_setting('app.current_user_area_id', true)
    );

-- ADMIN / MASTER Policy: Full unrestricted access
CREATE POLICY admin_master_visits_policy ON visits
    FOR ALL
    USING (
        current_setting('app.current_user_role', true) IN ('ADMIN', 'MASTER')
    );
