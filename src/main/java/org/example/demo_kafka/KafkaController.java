package org.example.demo_kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  CONTROLLER - Các API để test Kafka
 * ============================================================
 *
 *  Danh sách API:
 *
 *  [Demo cơ bản]
 *  GET /api/kafka/send?msg=XinChao         → Gửi 1 message (không key)
 *  POST /api/kafka/send                    → Gửi message qua POST body
 *
 *  [Demo Partition]
 *  GET /api/kafka/send-batch               → Gửi 9 message - xem phân phối round-robin
 *  GET /api/kafka/send-with-key            → Gửi message CÓ KEY - xem partition cố định
 *  POST /api/kafka/send-with-key           → Gửi message với key tùy chọn
 * ============================================================
 */
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaProducerService producerService;

    // ─────────────────────────────────────────────────────────────
    // API 1: Gửi message đơn - không có key (round-robin partition)
    // Test: http://localhost:8081/api/kafka/send?msg=XinChao
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/send")
    public ResponseEntity<Map<String, String>> guiMessageGet(@RequestParam String msg) {
        producerService.guiMessage(msg);
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Đã gửi! Xem console để thấy vào Partition nào.",
                "content", msg
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // API 2: Gửi qua POST
    // Body: { "msg": "Đơn hàng #001" }
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> guiMessagePost(@RequestBody Map<String, String> body) {
        String msg = body.getOrDefault("msg", "Không có nội dung");
        producerService.guiMessage(msg);
        return ResponseEntity.ok(Map.of("status", "OK", "content", msg));
    }

    // ─────────────────────────────────────────────────────────────
    // API 3: Gửi 9 message KHÔNG có key → quan sát round-robin
    //
    // Kết quả mong đợi trong console:
    //   msg1 → Partition 0
    //   msg2 → Partition 1
    //   msg3 → Partition 2
    //   msg4 → Partition 0  (lặp lại)
    //   ...
    //
    // Test: http://localhost:8081/api/kafka/send-batch
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/send-batch")
    public ResponseEntity<Map<String, Object>> guiBatchRoundRobin() {
        List<String> donHangList = List.of(
                "Đơn hàng #001 - Áo thun size M",
                "Đơn hàng #002 - Quần jean size 30",
                "Đơn hàng #003 - Giày Nike size 42",
                "Đơn hàng #004 - Túi xách da màu đen",
                "Đơn hàng #005 - Đồng hồ thông minh",
                "Đơn hàng #006 - Tai nghe Sony",
                "Đơn hàng #007 - Bàn phím cơ",
                "Đơn hàng #008 - Chuột gaming",
                "Đơn hàng #009 - Màn hình 27inch"
        );

        donHangList.forEach(producerService::guiMessage);

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "huongDan", "Xem console - 9 message phân phối round-robin vào 3 partitions",
                "soLuong", donHangList.size(),
                "donHang", donHangList
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // API 4: Gửi message CÓ KEY → quan sát partition cố định
    //
    // KEY quyết định partition bằng công thức: hash(key) % numPartitions
    // → Message cùng key LUÔN vào cùng 1 partition!
    //
    // Test: http://localhost:8081/api/kafka/send-with-key
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/send-with-key")
    public ResponseEntity<Map<String, Object>> guiMessageVoiKey() {
        // Mô phỏng: 3 khách hàng đặt nhiều đơn hàng khác nhau
        // → Tất cả đơn của cùng 1 khách PHẢI vào cùng 1 partition
        //   để đảm bảo thứ tự xử lý (đặt → thanh toán → giao)

        List<Map<String, String>> danhSach = new ArrayList<>();

        String[][] donHangData = {
                {"KH-VIET", "Đơn #V1 - iPhone 15"},
                {"KH-TRAN", "Đơn #T1 - Samsung S24"},
                {"KH-HUNG", "Đơn #H1 - Macbook Pro"},
                {"KH-VIET", "Đơn #V2 - Ốp lưng iPhone"},   // Khách VIET đặt thêm
                {"KH-TRAN", "Đơn #T2 - Sạc nhanh 65W"},    // Khách TRAN đặt thêm
                {"KH-HUNG", "Đơn #H2 - Magic Mouse"},       // Khách HUNG đặt thêm
                {"KH-VIET", "Đơn #V3 - Cáp USB-C"},         // Khách VIET đặt thêm lần 3
        };

        for (String[] data : donHangData) {
            String key = data[0];
            String msg = data[1];
            producerService.guiMessageVoiKey(key, msg);
            danhSach.add(Map.of("key", key, "message", msg));
        }

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "huongDan", "Xem console - các đơn cùng key sẽ VÀO CÙNG 1 PARTITION!",
                "giaiThich", "KH-VIET luôn → Partition X | KH-TRAN luôn → Partition Y | KH-HUNG luôn → Partition Z",
                "danhSach", danhSach
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // API 5: Gửi với key tùy chọn qua POST
    // Body: { "key": "KH-001", "msg": "Đơn hàng của khách 001" }
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/send-with-key")
    public ResponseEntity<Map<String, String>> guiMessageVoiKeyPost(@RequestBody Map<String, String> body) {
        String key = body.getOrDefault("key", "default-key");
        String msg = body.getOrDefault("msg", "Không có nội dung");
        producerService.guiMessageVoiKey(key, msg);
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "key", key,
                "message", "Đã gửi! Xem console để biết vào Partition nào.",
                "content", msg
        ));
    }
}