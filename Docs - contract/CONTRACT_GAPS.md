# CONTRACT GAPS

> Cập nhật 05/10/2026: auth/deletion/retention đã được chốt và triển khai trong contract v1.6; xem [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md). Các đề xuất chưa triển khai bên dưới vẫn là phạm vi mở, không phải bằng chứng đã hoàn thành.

> Ghi nhận các khoảng trống, điểm chưa rõ ràng, mâu thuẫn trong `PROJECT_CONTRACT.md` hoặc các yêu cầu nghiệp vụ chưa được bao phủ trong v1. File này dùng để thảo luận trong team, KHÔNG tự ý thay đổi contract trước khi chốt phương án ở đây.

---

## GAP-01: Tác động của `mark-rented` (HOST05) lên Listing Status

- **Mô tả:** Khi host dùng API HOST05 để đổi Room availability sang `RENTED` (vì khách cọc ngoài app), trạng thái của Listing tương ứng (nếu đang PUBLISHED) có tự động chuyển sang HIDDEN hay ARCHIVED không? Hay hệ thống chỉ dùng bộ lọc loại trừ Room không `AVAILABLE` ở API tìm kiếm?
- **Tham chiếu:** PROJECT_CONTRACT BR-13, BR-02
- **Đề xuất (Chờ duyệt):** API tìm kiếm mặc định đã loại Room != AVAILABLE. Listing vẫn ở trạng thái PUBLISHED để không mất terms_version, nhưng sẽ tự động báo "Hết phòng" trên UI. Khi phòng trống lại (HOST06), Listing tự động hiện lại trên map/search mà không cần duyệt lại.

---

## GAP-02: Pagination / Cursor khi load Message cũ nhất trong Chat

- **Mô tả:** Ở tính năng chat, Cursor chỉ hỗ trợ `after_sequence` (để lấy message mới) và `next_cursor` (để lấy message cũ). Tuy nhiên, khi vào phòng chat lần đầu, nếu người dùng muốn anchor ở tin nhắn chưa đọc đầu tiên, API CHAT04 (MessageCursorQuery) cần tham số gì để load list xung quanh điểm đó?
- **Tham chiếu:** PROJECT_CONTRACT §7.2, BR-11
- **Quyết định Phase 7 (27/09/2026):** Chưa hỗ trợ `around_sequence`; không xem đây là tham số đã triển khai. Dùng cửa sổ tin mới nhất hoặc `before_sequence`/`after_sequence` loại trừ biên, `limit` 1..50 default 20, kết quả luôn ASC. `has_more` theo hướng truy vấn; `next_cursor` chỉ trỏ về tin cũ. Đồng bộ tin mới dùng sequence cuối trong `data` làm `after_sequence` tiếp theo. UI anchor quanh tin chưa đọc là phần phát triển tiếp theo.

---

## GAP-03: Tên phòng hiển thị trong hội thoại khi Listing bị xóa

- **Mô tả:** `ConversationDTO` có trả về `room_title`, nhưng nếu Listing bị ARCHIVED hoặc xóa hẳn, title này sẽ rỗng nếu join động (JOIN). 
- **Tham chiếu:** PROJECT_CONTRACT §1.8, §2.12
- **Quyết định Phase 7 (27/09/2026):** Lưu `room_title` snapshot trực tiếp trong `conversations` khi tạo. Đổi/ẩn/archive listing không thay title lịch sử. Conversation đã tồn tại tiếp tục dùng được bởi thành viên ACTIVE; tạo mới chỉ khi listing PUBLISHED và `expires_at > now`. Listing đã được dùng không xóa cứng theo quy tắc retention hiện có.

---

## Phase 7: Phạm vi chat đã chốt để tích hợp

- Tạo/mở theo `(room_id, tenant_id)` duy nhất, không tự chat với phòng của mình. Create yêu cầu ACTIVE + email verified; đọc/list/read marker yêu cầu ACTIVE, thao tác trên một hội thoại yêu cầu membership. Gửi tin yêu cầu thêm email verified.
- REST POST create và read marker yêu cầu `Idempotency-Key` không trắng, tối đa 128 ký tự. Replay trả snapshot response gốc sau khi kiểm tra lại tài khoản/quyền; GET để lấy trạng thái hiện tại. Read marker không giảm và không vượt sequence mới nhất.
- STOMP `/ws` dùng access JWT ở HTTP Authorization header của handshake. SEND `/app/chat.send`; chỉ SUBSCRIBE `/user/queue/chat.messages`, `/user/queue/chat.acks`, `/user/queue/chat.errors`. Không dùng token trong query string.
- Chỉ hỗ trợ TEXT. IMAGE/SYSTEM vẫn là enum dự trữ và bị từ chối; chưa có upload/attach ảnh chat. `client_message_id` UUID được chuẩn hóa chữ thường; retry cùng payload trả lại MessageDTO, đổi payload với cùng khóa trả 409.
- ACK và sự kiện message chỉ phát sau commit. ACK trả MessageDTO; lỗi trả ProblemDTO và chưa có cam kết correlation tới một `client_message_id`. Client không nên đánh dấu một tin bất kỳ là thất bại chỉ vì nhận lỗi không có correlation; có thể retry cùng UUID hoặc đối chiếu REST history.
- Simple broker và rate limit nằm trong một process: Phase 7 chỉ hỗ trợ một backend instance. Chưa có durable broker, offline push hay cam kết delivery exactly-once qua mạng. Reconnect phải REST resync và deduplicate; dữ liệu message đã commit nằm trong MySQL.
- V11 tạo các bảng chat; không sửa V1–V10 đã có. OpenAPI được bổ sung cho chat, các phần API cũ ngoài chat vẫn cần đối chiếu riêng; không tuyên bố OpenAPI bao phủ toàn hệ thống.
- Hướng dẫn request/response, reconnect và giới hạn vận hành: [backend/CHAT.md](../backend/CHAT.md). Kết quả kiểm thử thực tế được ghi riêng trong báo cáo tiến độ, không suy ra từ trạng thái đã chốt contract.

## Đối chiếu response ngày 28/09/2026

**Đã chốt và kiểm chứng ngày 04/10/2026:** Theo yêu cầu người dùng, contract v1.5 thống nhất global HTTP JSON envelope `{statusCode,message,data}`, kể cả Chat/Admin/Notification; data lỗi là ProblemDTO. Android/OpenAPI đã nâng cấp; 205 test backend và 6 unit test Android đạt, HTTP workflow PASS. Xem [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md). Đoạn bên dưới chỉ mô tả lịch sử trước khắc phục.

`FormatRestResponse` đã xuất hiện lại trong workspace sau bản contract v1.1. Chat, Admin và Notification được loại khỏi wrapper trong các lượt 28–29/09; lỗi HTTP chung trả ProblemDTO theo RFC 9457. Success response của các module khác hiện vẫn có wrapper cũ và cần đối chiếu với Android/contract trong lượt đồng bộ riêng. Không dùng kết quả test các module trên để kết luận response toàn hệ thống đã thống nhất.

## Phase 8: Ranh giới đã kiểm chứng ngày 29/09/2026

- Admin moderation, account suspension, audit cùng transaction và notification inbox đã có test. Trạng thái P8 trước đây ghi hoàn thành chưa phản ánh việc thiếu outbox/Kafka/FCM, producer booking/payment/viewing và nghiệm thu scheduled jobs; đã sửa lịch thành đang thực hiện.
- BookingCase resolution hiện chỉ ghi kết luận. Chưa có quy tắc amount/ratio cho SPLIT và mapping sang settlement/refund; chưa chuyển tiền dựa trên decision này.
- Listing SUSPENDED không xuất hiện public và host không tự submit lại. Chưa có endpoint/quy tắc phục hồi listing sau moderation.
- Refresh session bị thu hồi khi khóa tài khoản; request dùng access JWT bị chặn trong thời gian tài khoản inactive. Chưa có cơ chế thu hồi riêng từng access JWT sau mở khóa; vẫn phụ thuộc TTL và trạng thái hiện tại.
- Chi tiết tích hợp: [backend/ADMIN_NOTIFICATIONS.md](../backend/ADMIN_NOTIFICATIONS.md). Không suy ra push offline hoặc mọi module nghiệp vụ đã có notification chỉ từ việc inbox hoạt động.
> **Cập nhật 03/10/2026:** Hiện trạng triển khai và các quyết định thay thế mô tả cũ nằm tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). FCM dùng MySQL outbox; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các mốc kiểm chứng trước đây là lịch sử, không phải nghiệm thu bản hiện tại.
