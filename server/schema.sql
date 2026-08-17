-- SQL Script to initialize GastroLink Cloud Sync tables in MySQL

CREATE DATABASE IF NOT EXISTS gastrolink;
USE gastrolink;

-- 1. Table to store user nutrition profile
CREATE TABLE IF NOT EXISTS user_profiles (
    userId VARCHAR(50) NOT NULL PRIMARY KEY,
    age INT,
    weight DOUBLE,
    height DOUBLE,
    goal VARCHAR(30),
    allergies TEXT,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Table to store order history
CREATE TABLE IF NOT EXISTS user_orders (
    orderId VARCHAR(50) NOT NULL PRIMARY KEY,
    userId VARCHAR(50) NOT NULL,
    orderDate VARCHAR(50),
    totalKcal INT,
    totalProtein INT,
    totalCarbs INT,
    totalFat INT,
    itemsJson TEXT,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
