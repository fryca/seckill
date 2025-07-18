-- 插入测试产品
INSERT INTO t_product (name, description, price, stock, status) VALUES 
('iPhone 15 Pro', '苹果最新旗舰手机', 7999.00, 100, 1),
('MacBook Pro 14', '专业级笔记本电脑', 14999.00, 50, 1),
('AirPods Pro', '主动降噪耳机', 1999.00, 200, 1);

-- 插入测试秒杀活动
INSERT INTO t_seckill_activity (name, description, product_id, seckill_price, seckill_stock, original_stock, start_time, end_time, status) VALUES 
('iPhone 15 Pro 秒杀', 'iPhone 15 Pro 限时秒杀', 1, 6999.00, 10, 100, '2024-01-01 10:00:00', '2024-12-31 23:59:59', 1),
('MacBook Pro 秒杀', 'MacBook Pro 限时秒杀', 2, 12999.00, 5, 50, '2024-01-01 10:00:00', '2024-12-31 23:59:59', 1),
('AirPods Pro 秒杀', 'AirPods Pro 限时秒杀', 3, 1499.00, 20, 200, '2024-01-01 10:00:00', '2024-12-31 23:59:59', 1); 