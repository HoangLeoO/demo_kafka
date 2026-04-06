package org.example.demo_kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 *  CONSUMER - "Người nhận thư"
 * ============================================================
 *
 *  Demo này có 2 cách listen:
 *
 *  ① Consumer nhóm (group consumer) - CÁCH PHỔ BIẾN NHẤT
 *     → 3 instance trong cùng 1 groupId
 *     → Kafka tự động phân chia: mỗi instance nhận 1 partition
 *     → 3 partitions + 3 consumers = xử lý song song hoàn toàn
 *
 *  ② Consumer chỉ định partition - CÁCH NÂNG CAO
 *     → 1 consumer chỉ đọc partition số 0
 *     → Dùng khi bạn muốn ưu tiên xử lý partition cụ thể
 *
 *  PHÂN CHIA CONSUMER TRONG GROUP:
 *  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
 *  │ Partition 0 │   │ Partition 1 │   │ Partition 2 │
 *  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
 *         │                 │                  │
 *   Consumer #1        Consumer #2        Consumer #3
 *   (concurrency=3, Spring tự tạo 3 thread)
 * ============================================================
 */
@Slf4j
@Service
public class KafkaConsumerService {

    // ─────────────────────────────────────────────────────────────
    // ① GROUP CONSUMER với concurrency = 3
    //    Spring tự tạo 3 thread, mỗi thread xử lý 1 partition
    //    → 3 message được xử lý CÙNG LÚC thay vì lần lượt!
    // ─────────────────────────────────────────────────────────────
    @KafkaListener(
            topics = "don-hang",
            groupId = "demo-group",
            concurrency = "3"   // ← Tạo 3 consumer thread song song
    )
    public void nhanMessage(ConsumerRecord<String, String> record) {
        String threadName = Thread.currentThread().getName(); // Xem thread nào đang xử lý

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📥 [CONSUMER] Nhận message mới!");
        log.info("   🧵 Thread     : {}", threadName);      // Consumer thread nào xử lý
        log.info("   📌 Partition  : {}", record.partition()); // Đến từ partition nào
        log.info("   📍 Offset     : {}", record.offset());    // Vị trí thứ mấy
        log.info("   🔑 Key        : {}", record.key());       // Có key không?
        log.info("   📦 Nội dung   : {}", record.value());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        xuLyDonHang(record);
    }

    // ─────────────────────────────────────────────────────────────
    // ② PARTITION-SPECIFIC CONSUMER (nâng cao)
    //    Consumer này CHỈ đọc partition 0 của topic "don-hang"
    //    Dùng groupId khác để không xung đột với consumer ① ở trên
    // ─────────────────────────────────────────────────────────────
    @KafkaListener(
            groupId = "monitor-group",   // Group riêng biệt → nhận lại TẤT CẢ message
            topicPartitions = @TopicPartition(
                    topic = "don-hang",
                    partitions = {"0"}   // Chỉ lắng nghe partition 0
            )
    )
    public void monitorPartition0(ConsumerRecord<String, String> record) {
        log.info("🔍 [MONITOR] Partition-0 monitor nhận: key='{}' | value='{}'",
                record.key(), record.value());
    }

    /**
     * Xử lý nghiệp vụ - trong thực tế đây là nơi bạn:
     * - Lưu vào database
     * - Gửi email thông báo
     * - Gọi API sang service khác
     * - Cập nhật trạng thái đơn hàng
     */
    private void xuLyDonHang(ConsumerRecord<String, String> record) {
        log.info("⚙️  [CONSUMER] Đang xử lý đơn hàng từ Partition-{}: '{}'",
                record.partition(), record.value());

        // Giả lập thời gian xử lý (100ms)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("✅ [CONSUMER] Xử lý hoàn tất đơn: '{}'", record.value());
    }
}