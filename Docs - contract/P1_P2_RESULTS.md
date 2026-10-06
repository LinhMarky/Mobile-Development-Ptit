# Kết quả xử lý P1/P2 — 04/10/2026

Phạm vi: năm mục ưu tiên P1/P2 trong mục 8 của `danhgiatoandienngay4thang10.md`. Các nhận xét trong bản đánh giá được đối chiếu với source hiện tại trước khi sửa.

## Kết quả theo yêu cầu

| Mục | Thay đổi và kiểm chứng |
| --- | --- |
| P1 — UserResolver | Thêm `auth/security/UserResolver`, dùng chung với `AccountAccessService` để lấy tài khoản từ database và chặn tài khoản không active/suspended. Thay các đoạn lấy người dùng lặp trong booking, catalog, interaction, media, notification, profile và yêu cầu xóa tài khoản. Đọc media công khai cho phép anonymous, nhưng vẫn kiểm tra tài khoản khi đã đăng nhập. `SecurityUtils` bỏ qua principal chưa authenticated và anonymous. |
| P1 — Notification trong Booking/Payment | Booking đã có `BookingTransitions`: giữ một nơi ghi lịch sử và thông báo cho hai thành viên khi trạng thái đổi. Bổ sung thông báo cho payer khi payment thành công, thất bại, hết hạn hoặc webhook muộn được hoàn tiền sandbox. Dùng `ref_type=booking` để mở đúng màn Android. Replay cùng event hoặc event mới cho một kết quả đã xử lý không tạo thêm thông báo kết quả đó. Inbox, outbox và trạng thái nghiệp vụ cùng transaction; rollback không để lại thông báo. |
| P1 — PaymentExpirationJob | Quét tối đa 100 payment PENDING quá hạn mỗi lượt; mặc định mỗi 60 giây, cấu hình qua `homely.payment.expiration-poll-ms`. Mỗi payment được xử lý trong transaction riêng, khóa room → booking → payment rồi đọc lại trạng thái và deadline. Lỗi một payment không chặn các payment tiếp theo. EXPIRED một lượt thanh toán vẫn giữ booking APPROVED và phòng HELD nếu hạn giữ còn hiệu lực; tenant có thể tạo lượt mới. Webhook thành công đến sau hạn được hoàn sandbox, không xác nhận cọc. Index `(status, expires_at)` đã có từ V10. |
| P2 — BookingService unit test | Thêm 22 lượt test: snapshot giá, hạn yêu cầu 24h/giữ 48h, tự đặt phòng, email chưa xác minh, duyệt sai trạng thái, tranh chấp giữ phòng, cả hai thứ tự bàn giao, quyền thành viên, cấm hủy sau xác nhận, hoàn đúng payment thành công và kiểm tra lại trạng thái khi job dùng candidate cũ. |
| P2 — Auth integration test | Thêm 13 kịch bản, chạy trên H2 và lặp lại trên MySQL: đăng ký/BCrypt/default role, xác minh một lần, resend, token hết hạn/sai mục đích, bật host, sai credentials/token type, rotation/replay/installation, đăng nhập lại cùng thiết bị, logout một/tất cả thiết bị, không thu hồi session người khác, suspended/deleted và hai request refresh/verify đồng thời. |

## Bằng chứng

- **180 test chạy, 0 failure, 0 error, 0 skipped** ở lượt 04/10/2026, log [p1-p2-verify.log](../backend/target/p1-p2-verify.log).
- Trong đó: `UserResolverTest` 8; `BookingServiceTest` 22; `PaymentExpirationJobTest` 1; `AuthIntegrationTest` 13; `AuthMysqlIntegrationTest` 13; `BusinessWorkflowTest` 24; `BusinessMysqlTest` 24. Các test chat/admin/security/idempotency hiện có cũng chạy lại và đạt.
- MySQL thực tại cổng kiểm thử 33317, schema riêng `homely_remediation_test`; Flyway V1–V16 và Hibernate schema validation đạt. Bộ auth dùng MockMvc với filters/controller/JWT/BCrypt/JPA thật; SMTP và client MinIO được giả lập, nên kết quả này không chứng minh gửi email hoặc push ra dịch vụ bên ngoài.
- Lượt verify chạy hết test thành công nhưng bước repackage bị Windows khóa JAR bởi backend kiểm thử cũ. Đã xác minh và dừng tiến trình đó ở cổng 18080; đóng gói lại thành công trong [p1-p2-package.log](../backend/target/p1-p2-package.log). Bước đóng gói lại dùng `-DskipTests` vì bộ test vừa chạy đã đạt.
- JAR: `backend/target/rental-0.0.1-SNAPSHOT.jar`.

## Chạy lại

Từ thư mục `backend`:

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd -B -DforkCount=0 test
```

Để chạy cả MySQL, đặt `HOMELY_TEST_MYSQL_URL`, `HOMELY_TEST_MYSQL_USER` và `HOMELY_TEST_MYSQL_PASSWORD` trỏ tới schema kiểm thử riêng trước khi chạy. Test có ghi dữ liệu; không dùng database ứng dụng. Khi thiếu URL, các test MySQL được bỏ qua và số test đã thực sự chạy sẽ thấp hơn lượt nghiệm thu ở trên.

## Đối chiếu bản đánh giá

Nhận xét H1 “không có test” và H5 “notification chưa được gọi” không còn đúng với source hiện tại. P1/P2 đã được hoàn tất theo bằng chứng trên. Format REST hiện hành vẫn là DTO trực tiếp và lỗi ProblemDTO theo PROJECT_CONTRACT v1.4; đề xuất bật lại wrapper ở P0 của bản đánh giá không được dùng làm căn cứ cho lần sửa này.

FCM đã có outbox/worker và hướng dẫn [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Gửi push thật vẫn cần Firebase project và cấu hình của người dùng; không nằm trong bằng chứng kiểm thử dịch vụ ngoài ở lượt này.
