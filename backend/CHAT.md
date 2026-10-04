# Phase 7 — Chat & WebSocket

Chạy backend bằng Java 17+ và Maven. Flyway tự chạy `V11__chat_tables.sql` sau V1–V10; các bảng mới là `conversations`, `messages`, `read_markers`. API trả DTO trực tiếp. Đặc tả REST nằm trong [openapi.yaml](../Docs%20-%20contract/openapi.yaml).

## REST

Mọi request dùng `Authorization: Bearer <access_token>`. POST phải có `Idempotency-Key` không trắng, tối đa 128 ký tự. Cùng khóa/cùng body trả snapshot response lần đầu; đổi body phải dùng khóa mới. Quyền và trạng thái tài khoản được kiểm tra lại trước replay.

| Method | Path | Dữ liệu |
| --- | --- | --- |
| POST | `/api/v1/conversations` | `{"room_id": 12}` → 201 với conversation đã tạo hoặc đã tồn tại |
| GET | `/api/v1/conversations?page=0&size=20` | Danh sách của tài khoản hiện tại, size tối đa 50 |
| GET | `/api/v1/conversations/{id}` | Chi tiết, `last_read_sequence`, `unread_count` |
| GET | `/api/v1/conversations/{id}/messages?limit=20` | Cửa sổ tin mới nhất, kết quả theo sequence tăng dần |
| POST | `/api/v1/conversations/{id}/read-marker` | `{"last_read_sequence": 8}` → marker không giảm, không vượt sequence mới nhất |

Lịch sử nhận `{data: [...], next_cursor: number|null, has_more: boolean}`. Tải tin cũ bằng `before_sequence=next_cursor`; đồng bộ tin mới bằng `after_sequence=<sequence cuối đã nhận>`. Không truyền hai cursor cùng lúc. `has_more` theo hướng đang đọc; `next_cursor` chỉ dùng cho cửa sổ mới nhất/tin cũ và luôn null khi đọc `after_sequence`. Chưa hỗ trợ `around_sequence`.

Create yêu cầu email verified và listing PUBLISHED chưa hết hạn; chủ phòng không được tự mở hội thoại với mình. Conversation đã tồn tại vẫn giữ title snapshot và lịch sử khi listing bị ẩn/archive. Chỉ hai thành viên đọc được dữ liệu; tài khoản inactive bị chặn. `unread_count` không tính tin do chính mình gửi.

## STOMP dành cho Android native

1. Mở WebSocket `/ws` với HTTP header `Authorization: Bearer <access_token>`. Dùng `wss://` ở môi trường triển khai. Không đưa token vào query string; không dùng refresh token. Không có SockJS.
2. Gửi STOMP CONNECT bình thường; không gửi thêm header `login`, `passcode`, `user`, `simpUser`, `Authorization` trong STOMP frame. Danh tính lấy từ handshake.
3. Subscribe `/user/queue/chat.messages`, `/user/queue/chat.acks`, `/user/queue/chat.errors`. Không thay `/user` bằng ID/email; không subscribe trực tiếp `/queue` hay `/topic`.
4. SEND `/app/chat.send`, `content-type:application/json`, ví dụ:

```json
{"conversation_id":10,"content":"Phòng còn trống không?","client_message_id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","content_type":"TEXT"}
```

Tin gửi yêu cầu thành viên ACTIVE + email verified; content không trắng và tối đa 5000 ký tự. Phase 7 chỉ nhận TEXT; IMAGE/SYSTEM là enum dự trữ và bị từ chối.

ACK tại `/user/queue/chat.acks` là MessageDTO đã lưu, có `id`, `conversation_id`, `sender_id`, `client_message_id`, `content`, `content_type`, `sequence`, `created_at`. Tin mới cũng được gửi tới `/user/queue/chat.messages` của cả hai bên **sau khi transaction commit**. ACK chỉ gửi về phiên đã thực hiện SEND.

Giữ nguyên UUID và body khi retry. UUID chuẩn hóa chữ thường; retry cùng tin trả lại MessageDTO cũ, không tạo tin mới hay phát lại event. Dùng UUID cũ cho body/người gửi khác nhận lỗi 409. Deduplicate ở UI bằng `id` hoặc `(conversation_id, sequence)`, vì người gửi có thể nhận cả ACK lẫn event.

Lỗi xử lý payload/nghiệp vụ nhận ProblemDTO qua `/user/queue/chat.errors`. Vi phạm giao thức/quyền destination có thể trả STOMP ERROR rồi đóng kết nối. Lỗi chưa có correlation bắt buộc tới UUID: đừng đánh dấu tùy ý một tin đang chờ là thất bại; retry UUID đó hoặc đối chiếu history. Token hết hạn/tài khoản bị khóa bị chặn ở cả inbound và trước khi nhận MESSAGE qua kết nối đang mở.

## Kết nối lại và vận hành

Lưu sequence cuối cho từng hội thoại. Sau reconnect và subscribe, gọi REST `after_sequence` lặp tới `has_more=false`, kết hợp tin realtime bằng dedup; sau đó retry các UUID chưa có ACK. Không coi STOMP receipt là xác nhận lưu database.

- Simple broker và rate limit trong process: chạy một backend instance cho Phase 7. Mở rộng nhiều instance cần broker/limiter dùng chung.
- Realtime là best effort; database lưu lịch sử bền vững, REST phục hồi tin bỏ lỡ. Chưa có outbox chat hoặc push offline trong phase này.
- `homely.chat.rest-requests-per-minute=120`: giới hạn HTTP theo tài khoản, lỗi 429 có `Retry-After: 60`.
- `homely.chat.messages-per-minute=60`: giới hạn SEND theo tài khoản, chung cho các socket.
- `homely.chat.allowed-origins`: danh sách origin HTTP(S) phân cách dấu phẩy nếu cần; không nhận wildcard. Client native không gửi Origin vẫn kết nối được.
- Giới hạn frame 32 KiB, buffer gửi 256 KiB, thời gian gửi 10 giây.

## Kiểm thử

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd test
```

Các test chat bao gồm controller/security chain, giao dịch JPA/concurrency bằng H2 và client STOMP kết nối server trên cổng local ngẫu nhiên. H2 không thay thế kiểm chứng MySQL.

Để chạy thêm migration MySQL, cấp một **schema test riêng** qua `HOMELY_TEST_MYSQL_URL`; tùy chọn `HOMELY_TEST_MYSQL_USER` (mặc định root), `HOMELY_TEST_MYSQL_PASSWORD`. `ChatMysqlMigrationTest` migrate và validate mọi migration hiện có (bao gồm V11), kiểm tra unique constraints rồi rollback dữ liệu mẫu; không dùng URL database ứng dụng. Không có URL thì test này được skip. Kết quả lần chạy thực tế được ghi trong `Docs - contract/codex24th9.md`.
