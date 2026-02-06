-- =====================================================
-- FinGaurd Database Schema - PostgreSQL Production
-- =====================================================
-- This file creates the initial database structure for PostgreSQL
-- Designed for a comprehensive financial tracking and fraud detection system
--
-- Features:
-- - User management with role-based access control
-- - Transaction tracking with fraud detection capabilities
-- - Comprehensive indexing for optimal performance
-- - Data integrity constraints and validation
-- - Audit trails and timestamps
-- - Extensible design for future enhancements
-- =====================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- USERS TABLE
-- =====================================================
-- Purpose: Store user account information and authentication data
-- Features: Role-based access, audit trails, security constraints
CREATE TABLE IF NOT EXISTS users (
    -- Primary identification
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Authentication fields
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Profile information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    
    -- Role and permissions
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN', 'MODERATOR')),
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    
    -- Security and audit fields
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    last_password_change TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Timestamps for audit trail
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    
    -- Preferences and settings
    timezone VARCHAR(50) DEFAULT 'UTC',
    currency VARCHAR(3) DEFAULT 'USD',
    language VARCHAR(5) DEFAULT 'en',
    
    -- Constraints for data integrity
    CONSTRAINT users_email_format CHECK (
        email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    ),
    CONSTRAINT users_username_format CHECK (
        username ~* '^[a-zA-Z0-9_]{3,50}$'
    ),
    CONSTRAINT users_username_length CHECK (
        length(username) >= 3 AND length(username) <= 50
    ),
    CONSTRAINT users_password_hash_length CHECK (
        length(password_hash) >= 60
    ), -- BCrypt minimum length
    CONSTRAINT users_first_name_length CHECK (
        length(trim(first_name)) >= 1 AND length(first_name) <= 100
    ),
    CONSTRAINT users_last_name_length CHECK (
        length(trim(last_name)) >= 1 AND length(last_name) <= 100
    ),
    CONSTRAINT users_phone_format CHECK (
        phone IS NULL OR phone ~* '^\+?[1-9]\d{1,14}$'
    ),
    CONSTRAINT users_age_check CHECK (
        date_of_birth IS NULL OR date_of_birth <= CURRENT_DATE - INTERVAL '13 years'
    ),
    CONSTRAINT users_failed_attempts_check CHECK (
        failed_login_attempts >= 0 AND failed_login_attempts <= 5
    ),
    CONSTRAINT users_currency_format CHECK (
        currency ~* '^[A-Z]{3}$'
    ),
    CONSTRAINT users_language_format CHECK (
        language ~* '^[a-z]{2}(-[A-Z]{2})?$'
    )
);

-- =====================================================
-- TRANSACTIONS TABLE
-- =====================================================
-- Purpose: Store financial transaction records with fraud detection
-- Features: Comprehensive transaction tracking, risk assessment, categorization
CREATE TABLE IF NOT EXISTS transactions (
    -- Primary identification
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    
    -- Transaction details
    amount DECIMAL(15, 2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    subcategory VARCHAR(50),
    description TEXT,
    merchant_name VARCHAR(255),
    merchant_category_code VARCHAR(10),
    
    -- Location and metadata
    location_latitude DECIMAL(10, 8),
    location_longitude DECIMAL(11, 8),
    location_address TEXT,
    country VARCHAR(2),
    state VARCHAR(50),
    city VARCHAR(100),
    
    -- Payment method and account information
    payment_method VARCHAR(50),
    account_number_masked VARCHAR(20),
    bank_name VARCHAR(100),
    
    -- Transaction dates
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    posted_date TIMESTAMP,
    settlement_date TIMESTAMP,
    
    -- Fraud detection and risk assessment
    is_fraud_flagged BOOLEAN DEFAULT false,
    fraud_risk_score DECIMAL(5, 4),
    fraud_confidence_level VARCHAR(20),
    fraud_reason_codes TEXT[],
    manual_review_required BOOLEAN DEFAULT false,
    manual_review_status VARCHAR(20),
    manual_review_notes TEXT,
    
    -- Status and processing
    status VARCHAR(20) DEFAULT 'COMPLETED',
    processing_fee DECIMAL(10, 2),
    exchange_rate DECIMAL(10, 6),
    original_amount DECIMAL(15, 2),
    original_currency VARCHAR(3),
    
    -- Reference and tracking
    reference_number VARCHAR(100),
    transaction_reference VARCHAR(100),
    parent_transaction_id UUID,
    recurring_transaction_id UUID,
    
    -- Audit and timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    
    -- Foreign key relationships
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_parent FOREIGN KEY (parent_transaction_id) 
        REFERENCES transactions(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_creator FOREIGN KEY (created_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_updater FOREIGN KEY (updated_by) 
        REFERENCES users(id) ON DELETE SET NULL,
    
    -- Data integrity constraints
    CONSTRAINT transactions_amount_positive CHECK (
        amount > 0
    ),
    CONSTRAINT transactions_type_valid CHECK (
        transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'INVESTMENT', 'LOAN')
    ),
    CONSTRAINT transactions_status_valid CHECK (
        status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')
    ),
    CONSTRAINT transactions_fraud_score_range CHECK (
        fraud_risk_score IS NULL OR (fraud_risk_score >= 0 AND fraud_risk_score <= 1)
    ),
    CONSTRAINT transactions_fraud_confidence_valid CHECK (
        fraud_confidence_level IS NULL OR fraud_confidence_level IN (
            'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
        )
    ),
    CONSTRAINT transactions_manual_review_status_valid CHECK (
        manual_review_status IS NULL OR manual_review_status IN (
            'PENDING', 'APPROVED', 'REJECTED', 'ESCALATED'
        )
    ),
    CONSTRAINT transactions_coordinates_valid CHECK (
        (location_latitude IS NULL AND location_longitude IS NULL) OR
        (location_latitude IS NOT NULL AND location_longitude IS NOT NULL AND
         location_latitude BETWEEN -90 AND 90 AND
         location_longitude BETWEEN -180 AND 180)
    ),
    CONSTRAINT transactions_currency_format CHECK (
        original_currency IS NULL OR original_currency ~* '^[A-Z]{3}$'
    ),
    CONSTRAINT transactions_country_format CHECK (
        country IS NULL OR country ~* '^[A-Z]{2}$'
    ),
    CONSTRAINT transactions_exchange_rate_positive CHECK (
        exchange_rate IS NULL OR exchange_rate > 0
    ),
    CONSTRAINT transactions_processing_fee_non_negative CHECK (
        processing_fee IS NULL OR processing_fee >= 0
    ),
    CONSTRAINT transactions_dates_logical CHECK (
        transaction_date <= COALESCE(posted_date, transaction_date) AND
        posted_date <= COALESCE(settlement_date, posted_date)
    )
);

-- =====================================================
-- CATEGORIES TABLE (Reference Data)
-- =====================================================
-- Purpose: Standardized transaction categorization
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    parent_category_id UUID,
    description TEXT,
    icon VARCHAR(100),
    color VARCHAR(7),
    is_active BOOLEAN DEFAULT true,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_category_id) 
        REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT categories_color_format CHECK (
        color IS NULL OR color ~* '^#[0-9A-Fa-f]{6}$'
    )
);

-- =====================================================
-- ACCOUNTS TABLE (Optional - for future enhancement)
-- =====================================================
-- Purpose: Store user's financial accounts (bank accounts, credit cards, etc.)
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    bank_name VARCHAR(100),
    account_number_masked VARCHAR(20),
    balance DECIMAL(15, 2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT accounts_type_valid CHECK (
        account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT', 'LOAN', 'OTHER')
    ),
    CONSTRAINT accounts_currency_format CHECK (
        currency ~* '^[A-Z]{3}$'
    )
);

-- =====================================================
-- PERFORMANCE INDEXES
-- =====================================================

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login);
CREATE INDEX IF NOT EXISTS idx_users_email_verification ON users(email_verification_token) 
    WHERE email_verification_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_password_reset ON users(password_reset_token) 
    WHERE password_reset_token IS NOT NULL;

-- Transactions table indexes
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_amount ON transactions(amount);
CREATE INDEX IF NOT EXISTS idx_transactions_fraud ON transactions(is_fraud_flagged) 
    WHERE is_fraud_flagged = true;
CREATE INDEX IF NOT EXISTS idx_transactions_risk_score ON transactions(fraud_risk_score) 
    WHERE fraud_risk_score IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions(user_id, transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_user_type ON transactions(user_id, transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_merchant ON transactions(merchant_name) 
    WHERE merchant_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_location ON transactions(country, state, city) 
    WHERE country IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_reference ON transactions(reference_number) 
    WHERE reference_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);

-- Categories table indexes
CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories(parent_category_id);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_categories_sort ON categories(sort_order);

-- Accounts table indexes
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_accounts_active ON accounts(is_active) WHERE is_active = true;

-- =====================================================
-- TRIGGERS FOR AUTOMATIC TIMESTAMP UPDATES
-- =====================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to relevant tables
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_transactions_updated_at ON transactions;
CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_accounts_updated_at ON accounts;
CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- INITIAL DATA POPULATION
-- =====================================================

-- Insert default admin user (password: admin123)
-- Password hash for 'admin123' with BCrypt strength 12
INSERT INTO users (
    username, email, password_hash, first_name, last_name, role, is_verified
) VALUES (
    'admin', 
    'admin@fingaurd.com', 
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj6cQq.8J1QO', 
    'Admin', 
    'User', 
    'ADMIN',
    true
) ON CONFLICT (email) DO NOTHING;

-- Insert default transaction categories
INSERT INTO categories (name, description, icon, color, sort_order) VALUES
    ('Food & Dining', 'Restaurants, groceries, and food-related expenses', '🍽️', '#FF6B6B', 1),
    ('Transportation', 'Gas, public transport, rideshare, parking', '🚗', '#4ECDC4', 2),
    ('Shopping', 'Retail purchases, clothing, electronics', '🛍️', '#45B7D1', 3),
    ('Entertainment', 'Movies, games, subscriptions, events', '🎬', '#96CEB4', 4),
    ('Bills & Utilities', 'Electricity, water, internet, phone bills', '💡', '#FFEAA7', 5),
    ('Healthcare', 'Medical expenses, pharmacy, insurance', '🏥', '#DDA0DD', 6),
    ('Education', 'Tuition, books, courses, training', '📚', '#98D8C8', 7),
    ('Travel', 'Hotels, flights, vacation expenses', '✈️', '#F7DC6F', 8),
    ('Salary', 'Regular employment income', '💰', '#82E0AA', 101),
    ('Freelance', 'Contract work, consulting income', '💼', '#85C1E9', 102),
    ('Investment', 'Dividends, capital gains, interest', '📈', '#F8C471', 103),
    ('Business', 'Business-related income and expenses', '🏢', '#BB8FCE', 104),
    ('Other', 'Miscellaneous transactions', '📦', '#A6ACAF', 999)
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- VIEWS FOR COMMON QUERIES
-- =====================================================

-- User transaction summary view
CREATE OR REPLACE VIEW user_transaction_summary AS
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    COUNT(t.id) as total_transactions,
    COALESCE(SUM(CASE WHEN t.transaction_type = 'INCOME' THEN t.amount ELSE 0 END), 0) as total_income,
    COALESCE(SUM(CASE WHEN t.transaction_type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) as total_expenses,
    COALESCE(SUM(CASE WHEN t.transaction_type = 'INCOME' THEN t.amount ELSE -t.amount END), 0) as net_balance,
    COUNT(CASE WHEN t.is_fraud_flagged = true THEN 1 END) as fraud_flagged_count,
    MAX(t.transaction_date) as last_transaction_date
FROM users u
LEFT JOIN transactions t ON u.id = t.user_id
WHERE u.is_active = true
GROUP BY u.id, u.username, u.email;

-- Monthly transaction trends view
CREATE OR REPLACE VIEW monthly_transaction_trends AS
SELECT 
    user_id,
    DATE_TRUNC('month', transaction_date) as month,
    transaction_type,
    COUNT(*) as transaction_count,
    SUM(amount) as total_amount,
    AVG(amount) as average_amount
FROM transactions
WHERE status = 'COMPLETED'
GROUP BY user_id, DATE_TRUNC('month', transaction_date), transaction_type
ORDER BY user_id, month DESC, transaction_type;

-- =====================================================
-- SECURITY AND PERMISSIONS
-- =====================================================

-- Create application user (if not exists)
-- Note: This would typically be done by a database administrator
-- DO NOT include actual credentials in schema files

-- Grant appropriate permissions
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO fingaurd_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO fingaurd_app;

-- =====================================================
-- SCHEMA COMPLETION
-- =====================================================

-- Log schema creation
DO $$
BEGIN
    RAISE NOTICE 'FinGaurd database schema created successfully!';
    RAISE NOTICE 'Tables created: users, transactions, categories, accounts';
    RAISE NOTICE 'Indexes, triggers, and views configured for optimal performance';
    RAISE NOTICE 'Default admin user and categories populated';
END $$;