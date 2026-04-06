# 🚀 Demo Apache Kafka với Spring Boot

> Demo đơn giản, dễ hiểu về cách Kafka hoạt động trong ứng dụng Spring Boot thực tế.
> Tập trung vào **Producer → Topic (3 Partitions) → Consumer** với các ví dụ thực tế về đơn hàng.

---

## 📖 Kafka là gì? (Giải thích đơn giản)

Kafka giống như **hệ thống bưu điện** trong lập trình:

```
[Người gửi đơn hàng]  →  [Hòm thư / Topic]  →  [Nhân viên xử lý]
    Producer                   Kafka                  Consumer
```

| Khái niệm | Giải thích dễ hiểu |
|-----------|-------------------|
| **Producer** | Người gửi tin nhắn vào Kafka |
| **Consumer** | Người nhận và xử lý tin nhắn |
| **Topic** | "Kênh" để phân loại tin nhắn (giống channel Slack) |
| **Partition** | Topic được chia thành nhiều phần để xử lý song song |
| **Key** | Khóa để quyết định message vào partition nào |
| **Offset** | Số thứ tự của tin nhắn trong partition (luôn tăng) |
| **Consumer Group** | Nhóm các consumer cùng nhau xử lý 1 topic |
| **Broker** | Máy chủ Kafka lưu trữ tin nhắn |

---

## 🏗️ Cấu trúc project

```
demo_kafka/
├── docker-compose.yml                        ← Chạy Kafka + Zookeeper + Kafka UI
├── src/main/resources/
│   └── application.properties                ← Cấu hình kết nối Kafka
└── src/main/java/org/example/demo_kafka/
    ├── DemoKafkaApplication.java             ← Điểm khởi chạy
    ├── KafkaTopicConfig.java                 ← Tạo topic "don-hang" với 3 partitions
    ├── KafkaController.java                  ← API endpoint nhận request HTTP
    ├── KafkaProducerService.java             ← Gửi message vào Kafka
    └── KafkaConsumerService.java             ← Nhận & xử lý message từ Kafka
```

---

## 🔄 Luồng hoạt động

```
Người dùng (Browser / Postman)
        │
        │  HTTP GET / POST
        ▼
KafkaController
        │
        │  producerService.guiMessage(msg)
        ▼
KafkaProducerService
        │
        │  kafkaTemplate.send("don-hang", key, message)
        ▼
┌───────────────────────────────────────────┐
│         TOPIC: "don-hang"                 │
│  ┌─────────────┬─────────────┬──────────┐ │
│  │ Partition 0 │ Partition 1 │Partition2│ │
│  │ [msg][msg]  │ [msg][msg]  │[msg][msg]│ │
│  └──────┬──────┴──────┬──────┴────┬─────┘ │
└─────────│─────────────│───────────│───────┘
          │             │           │
          ▼             ▼           ▼
     Consumer-T1  Consumer-T2  Consumer-T3
      (thread 1)   (thread 2)   (thread 3)
          │             │           │
          └─────────────┴───────────┘
               Xử lý SONG SONG
```

---

## 🗂️ Chi tiết về 3 Partitions

### Partition là gì?
Partition là cách Kafka **chia nhỏ topic** để xử lý song song — giống như làn đường trên cao tốc:

```
❌ 1 Partition (1 làn đường):        ✅ 3 Partitions (3 làn đường):
┌─────────────────────┐              ┌──────────┬──────────┬──────────┐
│ P0: [1][2][3][4][5] │              │P0: [1][4]│P1: [2][5]│P2: [3][6]│
└──────────┬──────────┘              └────┬─────┴────┬─────┴────┬─────┘
           │                              │          │           │
      Consumer #1                   Consumer 1  Consumer 2  Consumer 3
   (xử lý từng cái)               (song song — nhanh gấp 3x)
```

### Phân phối message vào Partition

| Cách gửi | Kafka phân phối như thế nào |
|----------|---------------------------|
| **Không có Key** | Round-robin: msg1→P0, msg2→P1, msg3→P2, msg4→P0... |
| **Có Key** | `hash(key) % 3` → cùng key luôn vào cùng partition |

**Tại sao dùng Key?**
> Đảm bảo **thứ tự xử lý** cho cùng 1 đối tượng.
> Ví dụ: Đơn hàng của khách `KH-VIET` phải xử lý đúng thứ tự: *đặt hàng → thanh toán → giao hàng*
> Nếu 3 bước này vào 3 partition khác nhau, thứ tự không được đảm bảo!

```
key="KH-VIET"  →  hash("KH-VIET") % 3 = 0  →  luôn vào Partition 0
key="KH-TRAN"  →  hash("KH-TRAN") % 3 = 1  →  luôn vào Partition 1
key="KH-HUNG"  →  hash("KH-HUNG") % 3 = 2  →  luôn vào Partition 2
```

---

## ⚡ Hướng dẫn chạy

### Bước 1: Khởi động Kafka (cần Docker Desktop)

```bash
docker-compose up -d
```

Kiểm tra các container đã chạy chưa:
```bash
docker ps
```

Sẽ thấy 3 container: `zookeeper` | `kafka` | `kafka-ui`

### Bước 2: Chạy Spring Boot

```bash
./gradlew bootRun
```

Spring Boot chạy ở **port 8081** (tránh xung đột với Kafka UI ở 8080).

---

## 🧪 Danh sách API để test

### [1] Gửi 1 message đơn giản (không key)
```
GET http://localhost:8081/api/kafka/send?msg=XinChao
```
> Quan sát console: message vào partition nào (thay đổi mỗi lần gọi - round-robin)

---

### [2] Gửi message qua POST
```
POST http://localhost:8081/api/kafka/send
Content-Type: application/json

{ "msg": "Đơn hàng #001" }
```

---

### [3] Gửi 9 message cùng lúc — demo Round-Robin
```
GET http://localhost:8081/api/kafka/send-batch
```

> **Quan sát console:** 9 message được phân phối đều vào 3 partitions theo vòng:
> ```
> msg#1 → Partition 0
> msg#2 → Partition 1
> msg#3 → Partition 2
> msg#4 → Partition 0  ← lặp lại
> ...
> ```
> 3 Consumer thread xử lý **song song cùng lúc**!

---

### [4] Gửi message có KEY — demo Partition cố định ⭐
```
GET http://localhost:8081/api/kafka/send-with-key
```

> **Quan sát console:** Mỗi khách hàng luôn vào cùng 1 partition dù gửi nhiều đơn:
> ```
> key='KH-VIET' | Đơn #V1  →  Partition 0
> key='KH-TRAN' | Đơn #T1  →  Partition 1
> key='KH-HUNG' | Đơn #H1  →  Partition 2
> key='KH-VIET' | Đơn #V2  →  Partition 0  ← cùng VIET → cùng P0!
> key='KH-HUNG' | Đơn #H2  →  Partition 2  ← cùng HUNG → cùng P2!
> ```

---

### [5] Gửi message với key tùy chọn
```
POST http://localhost:8081/api/kafka/send-with-key
Content-Type: application/json

{ "key": "KH-001", "msg": "Đơn hàng của khách 001" }
```

---

## 👀 Xem kết quả

### Cách 1: Log console (quan trọng nhất)

Sau khi gọi `/send-with-key`, console sẽ hiện:

```
📤 [PRODUCER] Gửi vào topic 'don-hang' với key='KH-VIET': Đơn #V1 - iPhone 15
✅ [PRODUCER] Gửi thành công! key='KH-VIET' → Partition [0] | Offset [12]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📥 [CONSUMER] Nhận message mới!
   🧵 Thread    : consumer-0-C-1   ← Thread nào đang xử lý
   📌 Partition : 0                ← Đến từ partition nào
   📍 Offset    : 12
   🔑 Key       : KH-VIET
   📦 Nội dung  : Đơn #V1 - iPhone 15
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚙️  [CONSUMER] Đang xử lý đơn hàng từ Partition-0: 'Đơn #V1 - iPhone 15'
✅ [CONSUMER] Xử lý hoàn tất đơn: 'Đơn #V1 - iPhone 15'
```

### Cách 2: Kafka UI — giao diện web trực quan

Mở trình duyệt: **http://localhost:8080**

- **Topics** → `don-hang` → xem 3 partitions và số message trong mỗi cái
- **Messages** → xem nội dung từng message, key, partition, offset
- **Consumers** → xem `demo-group` và `monitor-group` đang hoạt động

---

## 💡 Tại sao dùng Kafka?

| Vấn đề không có Kafka | Giải pháp với Kafka |
|----------------------|---------------------|
| Service A gọi thẳng B → B chậm thì A cũng chờ | A gửi vào Kafka → trả kết quả ngay, B xử lý riêng |
| B bị crash → mất toàn bộ data | Kafka lưu message → B restart xong đọc lại từ offset cũ |
| Nhiều request cùng lúc → B quá tải | 3 Consumer thread xử lý song song → không quá tải |
| 1 event cần notify nhiều nơi | 1 message → nhiều Consumer Group cùng đọc độc lập |
| Khó trace lỗi | Offset đánh số rõ ràng → biết xử lý đến đâu, lỗi ở đâu |

---

## 🛠️ Tech stack

| Thành phần | Phiên bản |
|-----------|----------|
| Java | 17 |
| Spring Boot | 3.3.1 |
| Spring Kafka | (tích hợp sẵn trong Spring Boot) |
| Apache Kafka | 7.4.0 (qua Docker) |
| Lombok | latest |
| Kafka UI | latest |
#   d e m o _ k a f k a  
 