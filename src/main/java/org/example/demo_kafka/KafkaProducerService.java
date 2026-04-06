package org.example.demo_kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * ============================================================
 *  PRODUCER - "Người gửi thư"
 * ============================================================
 *
 *  Producer có nhiệm vụ:
 *  → Nhận message từ Controller
 *  → Đẩy (publish) message vào TOPIC "don-hang"
 *
 *  ─── PARTITION & KEY ────────────────────────────────────────
 *  Kafka phân chia message vào partition dựa trên KEY:
 *
 *  Không có key  → Kafka phân phối round-robin (luân phiên):
 *                  msg1 → P0, msg2 → P1, msg3 → P2, msg4 → P0...
 *
 *  Có key        → Hash(key) % numPartitions = partition cố định:
 *                  key="VIET" → luôn vào P0
 *                  key="TRAN" → luôn vào P1
 *                  key="HUNG" → luôn vào P2
 *
 *  💡 Dùng key khi cần: các message cùng loại phải xử lý theo thứ tự
 *     Ví dụ: mọi đơn hàng của user "A" phải vào cùng 1 partition
 *            để đảm bảo thứ tự xử lý (đặt → thanh toán → giao hàng)
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "don-hang";

    /**
     * Gửi message KHÔNG có key → Kafka tự phân phối round-robin.
     * Thích hợp cho các message độc lập, không cần đảm bảo thứ tự.
     */
    public void guiMessage(String message) {
        log.info("📤 [PRODUCER] Gửi vào topic '{}' (không key - round-robin): {}", TOPIC, message);

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(TOPIC, message);

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("✅ [PRODUCER] Gửi thành công! → Partition [{}] | Offset [{}]",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ [PRODUCER] Gửi thất bại! Lỗi: {}", exception.getMessage());
            }
        });
    }

    /**
     * Gửi message CÓ key → Kafka dùng hash(key) để chọn partition cố định.
     * Thích hợp khi cần đảm bảo thứ tự xử lý cho cùng 1 đối tượng.
     *
     * Ví dụ: key = mã khách hàng → mọi đơn của khách đó vào cùng 1 partition
     *
     * @param key     key để xác định partition (vd: mã khách hàng, mã đơn hàng)
     * @param message nội dung tin nhắn
     */
    public void guiMessageVoiKey(String key, String message) {
        log.info("📤 [PRODUCER] Gửi vào topic '{}' với key='{}': {}", TOPIC, key, message);

        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(TOPIC, key, message);

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("✅ [PRODUCER] Gửi thành công! key='{}' → Partition [{}] | Offset [{}]",
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ [PRODUCER] Gửi thất bại! Lỗi: {}", exception.getMessage());
            }
        });
    }
}