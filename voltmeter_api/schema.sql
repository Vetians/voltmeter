-- =====================================================
-- Database Schema: VoltMeter
-- Import langsung ke phpMyAdmin
-- =====================================================

CREATE DATABASE IF NOT EXISTS voltmeter_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE voltmeter_db;

-- Tabel Users
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('surveyor', 'admin') DEFAULT 'surveyor',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Work Orders
CREATE TABLE IF NOT EXISTS work_orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    work_order_id VARCHAR(50) UNIQUE NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    assigned_to VARCHAR(50) NOT NULL,
    status ENUM('active', 'completed') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabel Customers
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(50) UNIQUE NOT NULL,
    work_order_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    power_va INT NOT NULL,
    tariff VARCHAR(20) NOT NULL,
    last_month_usage DOUBLE DEFAULT 0,
    last_meter_reading DOUBLE DEFAULT 0,
    latitude DOUBLE DEFAULT 0,
    longitude DOUBLE DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (work_order_id) REFERENCES work_orders(work_order_id) ON DELETE CASCADE
);

-- Tabel Meters
CREATE TABLE IF NOT EXISTS meters (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    meter_number VARCHAR(50) NOT NULL,
    meter_index INT DEFAULT 0,
    last_reading DOUBLE DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Tabel Meter Records
CREATE TABLE IF NOT EXISTS meter_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(50) UNIQUE NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    meter_number VARCHAR(50) NOT NULL,
    previous_reading DOUBLE DEFAULT 0,
    current_reading DOUBLE NOT NULL,
    usage_kwh DOUBLE DEFAULT 0,
    record_date DATE NOT NULL,
    record_time TIME NOT NULL,
    visit_status ENUM('TERBACA_NORMAL', 'RUMAH_KOSONG', 'HALANGAN') DEFAULT 'TERBACA_NORMAL',
    photo_path VARCHAR(255) NULL,
    latitude DOUBLE DEFAULT 0,
    longitude DOUBLE DEFAULT 0,
    notes TEXT,
    recorded_by VARCHAR(50) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

-- Insert Default Users (password: admin123)
-- Generate hash: php -r "echo password_hash('admin123', PASSWORD_DEFAULT);"
SET @hash = '$2y$10$vMGdkg9gzzS4a1w/FWwan.Ywb16/rJ96zo7VxSFOzPOKUWvhaQfr.';

INSERT INTO users (user_id, username, password, full_name, role) VALUES
('USR001', 'admin', @hash, 'Administrator', 'admin'),
('USR002', 'surveyor1', @hash, 'Budi Santoso', 'surveyor'),
('USR003', 'surveyor2', @hash, 'Siti Rahayu', 'surveyor');

-- Insert Sample Work Order
INSERT INTO work_orders (work_order_id, month, year, assigned_to) VALUES
('WO-202606-001', 6, 2026, 'USR002');

-- Insert Sample Customers
INSERT INTO customers (customer_id, work_order_id, name, address, power_va, tariff, last_month_usage, last_meter_reading, latitude, longitude) VALUES
('534210001', 'WO-202606-001', 'Ahmad Fauzi', 'Jl. Merdeka No. 10, Jakarta Selatan', 1300, 'R1', 120.5, 45230.5, -6.2615, 106.8106),
('534210002', 'WO-202606-001', 'Dewi Lestari', 'Jl. Sudirman No. 25, Jakarta Pusat', 2200, 'R1', 185.2, 67890.0, -6.2088, 106.8264),
('534210003', 'WO-202606-001', 'Rizki Pratama', 'Jl. Gatot Subroto No. 5, Bandung', 3500, 'R2', 250.0, 89000.0, -6.9175, 107.6191);

-- Insert Sample Meters
INSERT INTO meters (customer_id, meter_number, meter_index, last_reading) VALUES
('534210001', 'ML-001-2024', 0, 45230.5),
('534210002', 'ML-002-A', 0, 34500.0),
('534210002', 'ML-002-B', 1, 33390.0),
('534210003', 'ML-003-2024', 0, 89000.0);
