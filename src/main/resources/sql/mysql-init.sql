-- MySQL数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE seckill;

-- 产品表
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 秒杀活动表
CREATE TABLE IF NOT EXISTS t_seckill_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    product_id BIGINT NOT NULL,
    seckill_price DECIMAL(10,2) NOT NULL,
    seckill_stock INT NOT NULL,
    original_stock INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES t_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_no VARCHAR(32) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status INT NOT NULL DEFAULT 0, -- 0-待支付 1-已支付 2-已取消 3-已退款
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES t_seckill_activity(id),
    FOREIGN KEY (product_id) REFERENCES t_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户秒杀资格表
CREATE TABLE IF NOT EXISTS t_user_seckill_qualify (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    qualified BOOLEAN NOT NULL DEFAULT false,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    FOREIGN KEY (activity_id) REFERENCES t_seckill_activity(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO t_product (name, description, price, stock, status) VALUES 
('iPhone 15 Pro', '苹果最新旗舰手机', 7999.00, 100, 1),
('MacBook Pro 14', '专业级笔记本电脑', 14999.00, 50, 1),
('AirPods Pro', '主动降噪耳机', 1999.00, 200, 1);

INSERT INTO t_seckill_activity (name, description, product_id, seckill_price, seckill_stock, original_stock, start_time, end_time, status) VALUES 
('iPhone 15 Pro 秒杀', 'iPhone 15 Pro 限时秒杀', 1, 6999.00, 10, 100, '2024-01-01 10:00:00', '2024-12-31 23:59:59', 1),
('MacBook Pro 秒杀', 'MacBook Pro 限时秒杀', 2, 12999.00, 5, 50, '2024-01-01 10:00:00', '2024-12-31 23:59:59', 1),
('AirPods Pro 秒杀', 'AirPods Pro 限时秒杀', 3, 1499.00, 20, 200, '2024-01-01 10:00:00', '2024-12-31 23:59:59', 1); 