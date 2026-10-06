# Cấu hình Firebase Cloud Messaging cho Homely

Cập nhật 03/10/2026. Hiện chưa có Firebase project/credentials theo thông tin người dùng cung cấp. App và backend chạy được khi chưa cấu hình Firebase; inbox vẫn lưu trong MySQL. Chỉ bật gửi push thật sau các bước dưới đây.

## 1. Tạo project và ứng dụng Android

1. Tạo Firebase project trong Firebase Console. Google Analytics không bắt buộc cho luồng FCM của dự án.
2. Thêm Android app với package **`com.example.homely`**, đúng `applicationId` trong app Gradle.
3. Tải `google-services.json`, đặt tại **`Homely-android/app/google-services.json`**. Gradle chỉ áp dụng Google Services plugin khi file này tồn tại.
4. Build/cài lại ứng dụng. Dùng điện thoại có Google Play services hoặc emulator có image Google Play.
5. Đăng nhập, vào **Tài khoản → Cho phép thông báo**. Android 13+ cần cấp quyền thông báo. Token được đăng ký với `PUT /api/v1/profile/device-token` và cập nhật khi Firebase đổi token.

Tham khảo [thiết lập Firebase Android](https://firebase.google.com/docs/android/setup) và [FCM Android](https://firebase.google.com/docs/cloud-messaging/android/get-started).

## 2. Cấu hình backend

Bật Firebase Cloud Messaging API trong project. Tạo service account phù hợp cho server và lưu file JSON ở thư mục riêng ngoài repository. **Service account không được đưa vào APK, commit hoặc gửi qua chat.**

Trong PowerShell chạy backend:

```powershell
$env:FCM_ENABLED = 'true'
$env:FIREBASE_PROJECT_ID = 'your-project-id'
$env:GOOGLE_APPLICATION_CREDENTIALS = 'C:\private\homely-firebase-service-account.json'
java -jar backend/target/rental-0.0.1-SNAPSHOT.jar
```

Các biến MySQL, JWT, MinIO và SMTP vẫn cần theo hướng dẫn chạy dự án. Nếu dùng Docker, mount file ở chế độ read-only, đặt `GOOGLE_APPLICATION_CREDENTIALS` là đường dẫn **trong container**, đồng thời override `FCM_ENABLED`/`FIREBASE_PROJECT_ID`. Không COPY credential vào Docker image. Firebase Admin SDK dùng Application Default Credentials theo [hướng dẫn chính thức](https://firebase.google.com/docs/admin/setup).

### Docker Compose

Sau khi có Firebase project và service account, tại thư mục `Infra`:

```powershell
$env:FIREBASE_PROJECT_ID = 'your-project-id'
$env:FIREBASE_SERVICE_ACCOUNT_FILE = 'C:/private/homely-firebase-service-account.json'
docker compose -f compose.yaml -f compose.firebase.yaml config --quiet
docker compose -f compose.yaml -f compose.firebase.yaml up --build -d backend
```

Override yêu cầu project ID và file hiện có; mount read-only tại `/run/secrets/firebase-service-account.json`. Backend không khởi tạo gateway khi FCM_ENABLED=false; khi bật, thiếu project ID/credentials sẽ làm startup thất bại để không che lỗi cấu hình.

Metrics dành cho ADMIN: `homely.push.backlog`, `homely.push.failed`, `homely.push.attempts` qua `/actuator/metrics`. Kết quả retry/sent/failed được ghi sau transaction finish. Các metrics hỗ trợ chẩn đoán; giao hàng vẫn cần kiểm tra thiết bị.

## 3. Kiến trúc và hành vi

- Booking, chat, admin và lịch xem tạo inbox + `push_deliveries` trong cùng transaction MySQL. Rollback không để lại thông báo đẩy.
- Worker lấy tối đa 50 delivery/lượt, gửi ngoài transaction, khóa claim để tránh hai worker nhận cùng việc; lease 180 giây để khôi phục sau crash.
- Lỗi tạm thời được thử lại với thời gian chờ tăng dần; tối đa 8 lần, delivery quá 24 giờ bị kết thúc. Token `UNREGISTERED` bị vô hiệu hóa. Không ghi token/credential vào log.
- Worker tôn trọng `chat_push`/`transaction_push`; inbox vẫn còn nếu tắt push. `recommendation_push` là trường tương thích cũ, hiện không có producer gợi ý phòng.
- Payload data-only có `notification_id`, `recipient_id`, `ref_type`, `ref_id`. Android kiểm tra người nhận với tài khoản đang đăng nhập trước khi hiển thị. Không đưa nội dung chat vào thông báo ngoài màn hình khóa.
- Đăng xuất vô hiệu hóa thiết bị thuộc session ở server; Android xóa session cục bộ cả khi mất mạng. Thông báo cũ gửi đến tài khoản khác bị client bỏ qua. Chuyển tài khoản trên cùng installation thay quyền sở hữu token.
- Giao nhận là **at-least-once**: crash sau gửi nhưng trước lưu SENT có thể gửi lại. Android dùng cùng notification ID để thay thông báo cũ. Push chỉ báo thay đổi; màn hình luôn đọc trạng thái thật từ API.
- Chưa có Kafka trong luồng này. MySQL outbox đủ cho phạm vi triển khai một backend của đồ án.

## 4. Nghiệm thu trên thiết bị sau khi có cấu hình

1. Đăng nhập tenant ở điện thoại A, host ở B; kiểm tra token của cả hai đang active trong database.
2. Tenant tạo booking; host nhận thông báo. Host duyệt; tenant nhận thông báo và bấm mở đúng booking.
3. Gửi chat khi ứng dụng người nhận ở foreground và background; mở đúng hội thoại, chỉ thành viên đọc được.
4. Đặt/xác nhận/hủy lịch xem; kiểm tra inbox và push ở hai tài khoản.
5. Tắt quyền thông báo Android: không hiện push, inbox vẫn cập nhật. Tắt preference chat: delivery chat chuyển SKIPPED.
6. Đăng xuất A, đăng nhập tài khoản khác: push cho tài khoản cũ không được hiển thị. Cài lại app: đăng ký token mới.
7. Tạm ngắt mạng server rồi bật lại: retry thành công và không tạo thêm inbox. Token không hợp lệ phải chuyển FAILED và device inactive.
8. Ghi lại model thiết bị, phiên bản Android, thời điểm, `notification_id` và kết quả; không ghi raw token.

Các test tự động dùng gateway giả để kiểm tra transaction/retry/invalid token/preferences. Chúng **không chứng minh** Firebase đã giao thông báo tới điện thoại. Force-stop ứng dụng qua Android Settings cũng không phải trạng thái background thông thường; mở lại app trước khi thử tiếp.
