@echo off
echo 正在启动秒杀系统服务...

echo 1. 启动Docker服务...
docker-compose up -d

echo 2. 等待服务启动...
timeout /t 10 /nobreak > nul

echo 3. 检查服务状态...
docker-compose ps

echo 4. 启动Spring Boot应用...
echo 请在新窗口中运行: mvn spring-boot:run

echo 服务启动完成！
echo 访问地址:
echo - 应用: http://localhost:8080
echo - MySQL: localhost:3306
echo - Redis: localhost:6379
echo - Kafka: localhost:9092

pause 