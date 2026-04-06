# ============================================================
#  DOCKERFILE - Đóng gói ứng dụng thành Docker image
# ============================================================
#
#  Build: docker build -t demo-kafka .
#  Run:   docker run -p 8081:8081 demo-kafka
# ============================================================

# Dùng image Java 17 nhẹ (alpine)
FROM eclipse-temurin:17-jre-alpine

# Thư mục làm việc trong container
WORKDIR /app

# Copy file .jar đã build vào container
COPY build/libs/demo_kafka-0.0.1-SNAPSHOT.jar app.jar

# Mở port 8081
EXPOSE 8081

# Lệnh chạy khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]
