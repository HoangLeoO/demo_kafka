package org.example.demo_kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 *  DEMO KAFKA - Spring Boot
 * ============================================================
 *
 *  Luồng hoạt động (rất đơn giản):
 *
 *   [Người dùng gọi API]
 *        ↓
 *   [KafkaController] nhận request HTTP
 *        ↓
 *   [KafkaProducerService] gửi message vào TOPIC "don-hang"
 *        ↓
 *   [Kafka Broker] lưu trữ message
 *        ↓
 *   [KafkaConsumerService] tự động nhận & xử lý message
 *
 *  Để chạy demo:
 *  1. Chạy Kafka: docker-compose up -d
 *  2. Chạy Spring Boot: ./gradlew bootRun
 *  3. Gửi message: GET http://localhost:8081/api/kafka/gui -  mở giao diện web
 *     hoặc: GET http://localhost:8081/api/kafka/send?msg=XinChao
 * ============================================================
 */
@SpringBootApplication
public class DemoKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoKafkaApplication.class, args);
    }
}
