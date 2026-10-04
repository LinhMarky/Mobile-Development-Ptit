# Admin và thông báo trong ứng dụng

Phần này được kiểm chứng ngày 29/09/2026. API dùng base `/api/v1`, access JWT ở header `Authorization: Bearer ...`. Mọi POST dưới `/admin` và `/notifications` cần `Idempotency-Key` không trắng, tối đa 128 ký tự. Cùng key và cùng request trả snapshot kết quả cũ; đổi nội dung hoặc query phải dùng key mới. Tài khoản và quyền admin hiện tại trong database được kiểm tra trước replay.

## Admin

Các endpoint trả 200 với body rỗng khi thành công. `id` phải dương. Tham số lý do/ghi chú nằm ở query string và phải được URL-encode; đây là định dạng hiện có của controller.

| POST path | Tham số bắt buộc | Hành vi |
| --- | --- | --- |
| `/admin/listings/{id}/approve` | — | PENDING_REVIEW → PUBLISHED; đặt published_at, expires_at sau đúng 30 ngày và snapshot terms_version |
| `/admin/listings/{id}/reject` | `reason`, không trắng, tối đa 500 ký tự | Chỉ từ PENDING_REVIEW; lưu lý do trong audit, gửi thông báo cho host |
| `/admin/listings/{id}/suspend` | `reason`, tối đa 500 | PUBLISHED/HIDDEN → SUSPENDED; host không tự submit lại tin bị tạm ngưng |
| `/admin/users/{id}/suspend` | `reason`, tối đa 500 | Khóa tài khoản ACTIVE, đồng bộ status/cờ suspended và thu hồi các refresh session; không được tự khóa mình |
| `/admin/users/{id}/unsuspend` | — | Mở khóa tài khoản SUSPENDED hoặc cờ legacy; không hồi sinh tài khoản DELETED hay session đã thu hồi |
| `/admin/cases/{id}/resolve` | `decision`, `note` không trắng, tối đa 2000 | OPEN/IN_REVIEW → RESOLVED, ghi người xử lý và thời điểm, thông báo hai bên booking |
| `/admin/reports/{id}/resolve` | `note`, không trắng, tối đa 1000 | PENDING/REVIEWING → RESOLVED, thông báo người báo cáo |
| `/admin/reviews/{id}/hide` | `reason`, tối đa 500 | Ẩn review đang hiển thị và lưu audit |

Decision gồm `REFUND_TENANT`, `FORFEIT_DEPOSIT`, `SPLIT`, `NO_ACTION`. Endpoint case hiện **ghi nhận kết luận**, chưa thực hiện phân bổ/hoàn tiền. Không dùng response này để hiển thị rằng tiền đã được chuyển. Chưa có endpoint khôi phục listing SUSPENDED trong phần này.

Mỗi thao tác thay đổi trạng thái hợp lệ ghi audit trong cùng transaction; lỗi hoặc rollback không để lại audit/thông báo thành công. Gọi lại một chuyển trạng thái đã hoàn tất bằng key mới trả 409. Optimistic locking ngăn hai lần duyệt cùng commit. Reason/note được lưu trong audit JSON, không ghi nguyên nội dung vào log ứng dụng.

JWT lấy danh sách role được cấp trong database, gồm `ROLE_ADMIN` khi có; không tự thêm quyền TENANT. Admin đã bị gỡ role không thể dùng response cache cũ để vượt kiểm tra quyền. Cả status SUSPENDED và cờ suspended legacy đều chặn đăng nhập, refresh, request HTTP có JWT và chat theo guard hiện có. Mở khóa không phục hồi refresh session cũ; cơ chế access JWT hiện tại vẫn dựa vào TTL và trạng thái tài khoản, chưa có danh sách thu hồi access token riêng.

## Hộp thông báo

Response là DTO trực tiếp, không có wrapper statusCode/message/data. Lỗi dùng ProblemDTO `application/problem+json`.

| Method | Path | Kết quả |
| --- | --- | --- |
| GET | `/notifications?page=0&size=20` | PageResponse; size 1..50; sắp xếp created_at DESC, id DESC; không áp dụng sort tùy ý |
| GET | `/notifications/unread-count` | `{"unread_count": 2}` |
| POST | `/notifications/{id}/read` | NotificationDTO thuộc người gọi; không thuộc quyền sở hữu trả 404 |
| POST | `/notifications/read-all` | `{"updated": 2}`; số bản ghi đổi từ chưa đọc sang đã đọc |
| POST | `/notifications/mark-all-read` | Alias cũ, nên chuyển client sang `/read-all` |

NotificationDTO gồm `id`, `type`, `title`, `body`, `ref_type`, `ref_id`, `is_read`, `created_at`. Client nên xử lý được type mới. Thông báo lưu trong database cùng giao dịch admin; đây là hộp thông báo trong ứng dụng, chưa phải push FCM.

## Kiểm chứng và phần tiếp theo

Lệnh chạy từ thư mục backend: `.\apache-maven-3.9.9\bin\mvn.cmd verify`. Thiết lập `HOMELY_TEST_MYSQL_URL` tới schema test riêng để chạy thêm test Flyway; không đặt URL database ứng dụng. Lượt kiểm chứng gồm 74 test, trong đó 19 test mới cho Admin/Notification/auth; kết quả và giới hạn được ghi tại [codex24th9.md](../Docs%20-%20contract/codex24th9.md).

Phase 8 tổng thể còn thiếu outbox, Kafka consumer và FCM delivery/retry, cũng như việc nối đầy đủ các sự kiện booking/payment/viewing vào thông báo. Các scheduled job cũ chưa được nghiệm thu toàn diện trong lượt này. Chưa có kiểm chứng concurrency của Admin trên MySQL hoặc MySQL 8.4; test giao dịch dùng H2, MySQL 8.0.45 dùng để validate migration và chạy ứng dụng.
