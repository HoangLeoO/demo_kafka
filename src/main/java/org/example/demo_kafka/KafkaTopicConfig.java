package org.example.demo_kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * ============================================================
 *  KAFKA TOPIC CONFIG - Cấu hình Topic
 * ============================================================
 *
 *  Class này chạy khi Spring Boot khởi động.
 *  Nó tự động tạo topic "don-hang" với:
 *  - 3 Partitions  → xử lý song song, tăng throughput
 *  - 1 Replica     → phù hợp môi trường dev (chỉ 1 broker)
 *
 *  ⚠️  Trong Production nên dùng replica = 3 (3 máy chủ Kafka)
 *
 *  Hình dung Partition như "làn đường trên cao tốc":
 *  - 1 partition = 1 làn đường → xe xếp hàng 1 dãy
 *  - 3 partitions = 3 làn đường → 3 xe chạy song song → nhanh 3x
 * ============================================================
 */
@Configuration
public class KafkaTopicConfig {

    private static final String TOPIC_NAME = "don-hang";
    private static final int NUM_PARTITIONS = 3;
    private static final int REPLICATION_FACTOR = 1; // Dev: 1 broker nên chỉ được replica = 1

    @Bean
    public NewTopic donHangTopic() {
        return TopicBuilder
                .name(TOPIC_NAME)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .build();
    }
}
