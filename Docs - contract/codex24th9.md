# Nhận định dự án Homely — 24/09/2026

## 1. Phạm vi và thời điểm đánh giá

File này được tạo **trước khi sửa mã nguồn** theo yêu cầu của người dùng. Đã đọc toàn bộ nội dung hiện có của 9 file: `00_README.md`, `BE_SPEC.md`, `CONTRACT_GAPS.md`, `CHANGELOG.md`, `FE_SPEC.md`, `openapi.yaml`, `PROJECT_CONTRACT.md`, `schedule.md`, `TEST_PLAN.md`; đồng thời đối chiếu mã nguồn backend, Android và thư mục Infra.

“Đọc toàn bộ” ở đây là toàn bộ nội dung thực tế được lưu trên máy. Không thể coi các phần bị mất trong tài liệu là đã đọc hoặc tự khôi phục bằng suy đoán. Những câu hướng dẫn agent, đề xuất ngân sách token hay yêu cầu đánh số dòng nằm trong tài liệu được xem là nội dung cần đánh giá, không thay thế yêu cầu hiện tại của người dùng.

## 2. Nhận định tổng thể

Homely có mục tiêu sản phẩm rõ: tìm phòng → liên hệ/hẹn xem → yêu cầu thuê → giữ phòng → cọc thử nghiệm → bàn giao → đánh giá. Việc tách Room vật lý khỏi Listing, dự kiến lưu snapshot điều khoản và xử lý tranh chấp là hướng thiết kế hợp lý. Modular monolith phù hợp với giai đoạn hiện tại; chưa cần tách microservice.

Tuy nhiên, dự án hiện vẫn ở giai đoạn nền tảng, **chưa có luồng nghiệp vụ đầu cuối chạy được để nghiệm thu**. Tài liệu thể hiện tham vọng lớn hơn đáng kể so với mã nguồn. Không nên dùng số lượng class, thư mục `target`, hay nhãn “hoàn thành” trong lịch để kết luận một phase đã đạt yêu cầu.

Ưu tiên của tôi là làm phần đã có trở nên đúng, kiểm chứng được, rồi hoàn thiện từng luồng nhỏ xuyên suốt backend–Android. Việc viết tiếp hàng loạt module trên nền xác thực và contract còn lệch sẽ tạo nhiều chi phí sửa lại.

## 3. Hiện trạng kiểm chứng từ workspace

| Phần | Bằng chứng | Đánh giá trước khi sửa code |
| --- | --- | --- |
| Backend nền tảng | `backend/pom.xml`, `common/`, Flyway V1–V3 | Có Spring Boot, JPA, Security, DTO lỗi, idempotency; cần sửa và kiểm thử trước khi nghiệm thu P0. |
| Auth | Controller/service/entity đăng ký, đăng nhập, refresh, xác minh, profile | Đã có đáng kể mã P1, nhưng session và một số boundary bảo mật chưa hoàn chỉnh. |
| Catalog | Room, Listing, Fee, Amenity, Wishlist cùng V3 | P2 đã được triển khai một phần, trái với lịch ghi “chưa bắt đầu”. Search mới ở mức cơ bản; còn thiếu kiểm soát công khai và nhiều ràng buộc. |
| P3–P8 | Không có source triển khai các package media, interaction, booking, payment, chat, notification, admin | Chưa có bằng chứng hoàn thành các module này. Hàm duyệt Listing trong service chưa tạo thành luồng admin đầy đủ. |
| Android | `MainActivity.java`, `activity_main.xml` | Skeleton Hello World; chưa có Retrofit, màn hình nghiệp vụ, ViewBinding/MVVM hay quyền INTERNET trong manifest. |
| Infra | Thư mục `Infra` trống; chưa tìm thấy Docker Compose/Dockerfile của dự án | Chưa có cấu hình tái tạo môi trường theo thiết kế MySQL/MinIO/Kafka. |
| Kiểm thử | Backend chưa có `src/test`; Android chỉ có test mẫu | Chưa có bằng chứng cho concurrency, phân quyền hay E2E. |
| Quản lý phiên bản | Workspace và các thư mục dự án đều không được Git nhận là repository | Chưa thể dựa vào commit/diff để truy vết trạng thái gốc; không tự khởi tạo hoặc push repository. |

Build thực tế được kiểm tra sau phần nhận định này; kết quả sẽ cập nhật ở cuối file, không giả định sẵn là đạt.

## 4. Chất lượng và khoảng trống tài liệu

### 4.1 Contract bị mất nội dung — vấn đề ưu tiên cao nhất

`PROJECT_CONTRACT.md` chứa nguyên văn `NOTE: The output was truncated because it was too long`, nhiều đoạn `to include a line number...`, bảng đứt giữa chừng và các khối nội dung ghép sai vị trí. Phần “13 nhóm chức năng” chỉ còn F01 và một đoạn F02; BR-02 bị ngắt, BR-03 đến BR-18 không có đầy đủ dù các tài liệu khác viện dẫn. Các link `CONTRACT_PART1_API_DATA_ENUM.md`, `CONTRACT_PART2_BUSINESS_RULES_AUTH.md`, `CONTRACT_PART3_OPS_QUALITY.md` không trỏ tới file hiện có trong bộ tài liệu.

Đây là lỗi của nội dung file, không phải chỉ là công cụ đọc cắt màn hình. Cần tìm bản gốc đầy đủ trước khi chốt các state machine và quy tắc tài chính. Không xóa những dấu vết này rồi coi contract đã được khôi phục.

### 4.2 OpenAPI chưa đủ để làm hợp đồng tích hợp

`openapi.yaml` hiện chỉ có 6 path: login, refresh, rooms, listings, bookings và approve booking. Một số lệch đã thấy:

- Server dùng `/v1`, mã controller hiện dùng cấu hình `/api/v1`.
- LoginRequest thiếu `installation_id`, `device_name` so với phần contract còn đọc được.
- RoomCreateRequest thiếu address/location và thiếu ràng buộc bắt buộc tương ứng.
- OpenAPI trả DTO trực tiếp nhưng `FormatRestResponse` bọc kết quả thành `statusCode/message/data`.
- Search khai báo `limit/offset`, code nhận Spring `Pageable`.
- `rent_vnd` là number trong OpenAPI, FE_SPEC yêu cầu chuỗi số nguyên.
- ProblemDetails thiếu `code`, `trace_id`, `timestamp` so với PROJECT_CONTRACT.

Chưa nên sinh Android client từ bản này rồi coi đó là API đã ổn định. Cần chọn và đồng bộ giao thức tại một lần cập nhật có thể review; không âm thầm đổi tất cả endpoint trong lúc sửa lỗi nền tảng.

### 4.3 Test plan có mâu thuẫn cần chốt

- C01 yêu cầu chỉ một Booking PENDING thành công cho một phòng; C02 lại giả định tồn tại hai Booking PENDING để chủ duyệt. Generated unique index trong phần contract còn lại chỉ khóa toàn phòng ở APPROVED/CONFIRMED, còn PENDING ngăn trùng theo tenant/phòng. C01 cần phân biệt cùng tenant với hai tenant khác nhau.
- C03 diễn đạt webhook trùng cho cùng payment có thể dẫn tới Refund. Cần phân biệt thông báo trùng của cùng khoản tiền với khoản tiền khác không thể phân bổ, tránh hoàn nhầm một thanh toán hợp lệ.
- Ma trận “mọi API phải 401 nếu không token” không áp dụng cho public discovery/auth. Quy định thiếu/trùng Idempotency-Key cũng phải phân biệt replay hợp lệ với body khác hoặc request đang xử lý.

### 4.4 Kế hoạch cần cập nhật theo bằng chứng

P0/P1 đang được ghi hoàn thành dù chưa có test, Infra trống và session chưa nối đầy đủ. P2 có code nhưng lịch chưa phản ánh. BE_SPEC/schedule yêu cầu Boot 3.5.x, `pom.xml` thực tế dùng 3.3.2. Đây là chênh lệch cấu hình xác định được; chưa đánh giá nâng phiên bản là cần thiết cho lượt sửa này. Ước lượng token trong lịch chỉ là gợi ý lập kế hoạch, không phải bằng chứng chất lượng hay tiến độ.

## 5. Các vấn đề mã nguồn cần ưu tiên

| Mức | Vấn đề và bằng chứng | Tác động |
| --- | --- | --- |
| P0 | `IdempotencyFilter` đăng ký ở `HIGHEST_PRECEDENCE + 10`, lấy principal trước Security; key không có body hash, chưa xử lý expiry | Có thể dùng scope `anonymous` và replay trước xác thực; request khác nội dung có thể nhận kết quả cũ. |
| P0 | `TokenService` tạo access/refresh JWT cùng kiểu kiểm tra; decoder chưa phân biệt mục đích token | Refresh token có nguy cơ được dùng như bearer để vào endpoint chỉ cần authenticated. |
| P1 | `ProfileController`/`UserService` trả entity `NotificationPreference`, có quan hệ tới `User` | `User` đã có WRITE_ONLY cho password và JsonIgnore cho refreshToken, nên chưa kết luận lộ hai secret này. Tuy vậy response đưa thêm dữ liệu hồ sơ ngoài mục đích; cần DTO whitelist và test serialization. |
| P1 | `ListingService.getListingDetail()` đọc bằng `findById`; `getFees()` không kiểm tra trạng thái công khai | Người chưa đăng nhập có thể đọc tin chưa duyệt và phí qua ID. |
| P1 | Catalog chưa thực thi đầy đủ email verified, validation địa chỉ/amenity và bảo vệ điều khoản | Có thể ghi dữ liệu không đáp ứng yêu cầu còn hiện rõ trong tài liệu. |
| P1 | Auth dùng refresh token trực tiếp trên `users`, chưa sử dụng session table đúng thiết kế; logout chưa đủ thu hồi access JWT | Chưa thể nghiệm thu quản lý nhiều thiết bị, rotation và thu hồi phiên. |
| P1 | Xác minh email phân tán qua controller/service, thay đổi user không được gói cùng việc tiêu thụ token | Cần transaction rõ ràng, test token không dùng lại; không phụ thuộc persistence context mở ở HTTP. |
| P1 | Security filter chưa thống nhất ProblemDTO; malformed JSON có thể vào catch-all 500 | Android khó phân biệt lỗi nhập liệu, hết phiên và lỗi máy chủ. |
| P2 | Tìm kiếm/lọc/geo, media, admin duyệt tin và Android chưa tạo thành hành trình sử dụng | Chưa có demo sản phẩm hoàn chỉnh dù đã có API nền. |

## 6. Hướng tiếp tục thực hiện sau khi lưu file này

Lượt làm việc này ưu tiên một đợt củng cố P0–P2: sửa việc scope/replay idempotency, tách mục đích access/refresh token và DTO dữ liệu tài khoản, chặn đọc public tin chưa công bố, cải thiện lỗi đầu vào rõ ràng và thêm test hồi quy cho các hành vi được sửa. Những thay đổi được giới hạn vào hành vi có bằng chứng; các vấn đề chưa chốt được ghi trong CONTRACT_GAPS.

Thứ tự phát triển tiếp theo tôi đề xuất:

1. Khôi phục contract đầy đủ, thống nhất response/pagination/money/error và đối chiếu OpenAPI.
2. Hoàn thiện Auth/session, Catalog/Media, quyền admin và luồng chủ đăng → duyệt → khách tìm/xem. Xây Android tối thiểu cho chính luồng này, có loading/empty/error.
3. Booking và các bài test giữ phòng đồng thời, snapshot điều khoản, hết hạn. Chốt C01 trước khi viết state machine.
4. Payment sandbox, webhook dedup/refund/late settlement; nghiệm thu bằng test tích hợp với database thực.
5. Viewing, review/report, chat và thông báo theo phụ thuộc thực tế; Review hoàn chỉnh phải dựa trên Booking COMPLETED, nên không coi P4 độc lập hoàn toàn với P5.
6. Kiểm thử E2E, Flyway trên database sạch, cấu hình môi trường và hướng dẫn chạy lặp lại.

Tôi đánh giá thiết kế sản phẩm có cơ sở tốt, nhưng độ tin cậy hiện bị giới hạn bởi contract hỏng và thiếu kiểm thử. Nên đo tiến độ bằng hành trình chạy được cùng bằng chứng test, không đưa phần trăm hoàn thành khi chưa có baseline nghiệm thu.

## 7. Kết quả thực hiện sau đánh giá

Chưa bắt đầu sửa mã nguồn tại thời điểm tạo file. Phần này sẽ được cập nhật sau khi triển khai và chạy kiểm tra; các phần nhận định phía trên giữ lại hiện trạng trước sửa.

## 8. Tiếp nối ngày 26/09/2026 — Phase 7

Người dùng đã yêu cầu cụ thể triển khai Phase 7 (Chat & WebSocket). Khi tiếp tục, workspace đã có thêm mã nguồn Media/Interaction/Booking/Payment, các migration đến V10 và contract có các mục chat `[RECOVERED]`. Vì vậy các nhận định ngày 24/09 phía trên là ảnh chụp hiện trạng trước đó, không dùng để kết luận rằng các module này vẫn chưa tồn tại.

Phạm vi lượt này: hội thoại tenant–host theo phòng, lịch sử tin nhắn có cursor, gửi TEXT qua STOMP, xác thực và phân quyền từng thành viên, chống trùng `client_message_id`, read marker, migration V11 và kiểm thử. Phần IMAGE/SYSTEM chưa có quy tắc kiểm chứng quyền media/server message đầy đủ nên không nhận như tin TEXT; giới hạn này được ghi trong contract cho Android tích hợp đúng.

### Kết quả triển khai và kiểm chứng 27–28/09/2026

- Đã có REST tạo/mở hội thoại, danh sách/chi tiết, lịch sử với cursor và read marker tăng đơn điệu. Title phòng được lưu snapshot; unread không tính tin do chính người đọc gửi.
- STOMP `/ws` xác thực access JWT tại handshake, gửi qua `/app/chat.send`, nhận message/ACK/lỗi qua ba queue riêng. Kiểm tra membership, trạng thái ACTIVE và email verified theo từng thao tác; chặn refresh token, giả mạo danh tính và subscribe destination của người khác.
- Gửi tin khóa conversation và cấp sequence trong cùng transaction. UUID trùng với cùng nội dung trả tin cũ; khác nội dung/người gửi trả 409. Event chỉ phát sau commit; rollback không phát tin. READ_COMMITTED tránh đọc snapshot cũ sau khi chờ khóa trên MySQL.
- Bổ sung migration `V11__chat_tables.sql`, unique constraints cho hội thoại, sequence và UUID. Các POST chat sử dụng idempotency, kiểm tra lại quyền trước khi replay; REST/STOMP có giới hạn tần suất.
- Đồng bộ phần chat trong PROJECT_CONTRACT, CONTRACT_GAPS, CHANGELOG và OpenAPI; hướng dẫn client tại [backend/CHAT.md](../backend/CHAT.md). OpenAPI đã được parse YAML, kiểm tra 42 tham chiếu nội bộ của 10 path và phần `x-stomp`; đây không phải xác nhận toàn bộ API cũ đã khớp code.

### Bằng chứng kiểm chứng

Lượt `mvn verify` ban đầu đạt **54 test, 0 lỗi, 0 bỏ qua**, tạo được `backend/target/rental-0.0.1-SNAPSHOT.jar`. Test bao gồm security/controller, phân quyền queue, kết nối WebSocket/STOMP thực, gửi đồng thời, UUID dedup, cursor, marker, rollback, lỗi chung và HTTP idempotency. Migration V1–V11 đã chạy và validate trên **MySQL 8.0.45** bằng instance/schema test riêng; kiểm tra các unique constraints đã đạt. JAR đã khởi động thành công với schema này; log tại `backend/target/phase7-smoke.log`.

Rà soát cuối phát hiện và sửa việc timestamp của ACK có độ chính xác lớn hơn DATETIME(3), cùng việc cắt preview có thể tách đôi emoji. Đã bổ sung kiểm tra dữ liệu ACK/event/history giống nhau và regression cho emoji. Ngày 28/09/2026, lượt verify cuối đạt **55 test, 0 lỗi, 0 bỏ qua**; Flyway validate toàn bộ V1–V13 trên MySQL 8.0.45 và backend đã được đóng gói lại. Lệnh chạy: `mvn -q -l target/phase7-final-verify.log verify` với `HOMELY_TEST_MYSQL_URL` trỏ schema test riêng; báo cáo từng test nằm trong `backend/target/surefire-reports`. Bản JAR cuối đã khởi động thành công với schema V13: `/actuator/health` trả 200, `/api/v1/conversations` và `/ws` trả 401 khi thiếu token; log tại `backend/target/phase7-final-smoke.log`.

Trong lượt tiếp tục, workspace có thêm phần Admin/Notification và migration V12–V13. Đã sửa tham chiếu enum không tồn tại `ListingStatus.PENDING` thành `PENDING_REVIEW` để backend biên dịch, đồng thời guard chat kiểm tra cả `status` và cờ `suspended` mới. Test migration kiểm tra V11 đã áp dụng và không còn migration chờ, thay vì buộc version cuối luôn là 11. Lượt kiểm tra cũng phát hiện lớp wrapper response và bộ xử lý lỗi cũ đã xuất hiện lại. Đã loại chat khỏi wrapper, khôi phục ProblemDTO cho lỗi chung, xử lý JSON/validation thành 400, lỗi đăng nhập thành 401 và giữ lỗi nội bộ ở 500 với nội dung an toàn. Các thay đổi tương thích này không phải nghiệm thu đầy đủ Phase 8.

### Nhận định và giới hạn

Backend chat TEXT đã có các lớp bảo vệ và cơ chế khôi phục cần thiết để Android tích hợp. Bước tiếp theo nên là nối UI Android với REST/STOMP theo CHAT.md, kiểm tra mất mạng/reconnect và thử nghiệm trên MySQL 8.4 đúng môi trường mục tiêu. Concurrency service hiện được kiểm thử bằng H2; test MySQL kiểm chứng migration và ràng buộc dữ liệu, chưa thay thế bài tải đồng thời end-to-end trên MySQL.

Phase 7 chạy một backend instance với simple broker trong process. Tin đã commit nằm trong MySQL; realtime là best effort, client phải REST resync/deduplicate. Chat ảnh, SYSTEM message, `around_sequence`, push offline và broker nhiều instance chưa nằm trong phần hoàn thành. Kết quả Phase 7 không đồng nghĩa các module trước hoặc toàn bộ sản phẩm đã được nghiệm thu.

## 9. Tiếp nối ngày 28–29/09/2026 — Admin và hộp thông báo (một phần Phase 8)

### Nhận định trước khi tiếp tục

Sau Phase 7, workspace đã có controller/service Admin, Notification và migration V12–V13. Tuy nhiên, nhãn hoàn thành P8 trong lịch chưa được hỗ trợ bởi bằng chứng: một số hành động chưa kiểm soát đầy đủ chuyển trạng thái, audit và thông báo chưa đi cùng giao dịch, JWT chưa phản ánh đúng role admin, trạng thái khóa tài khoản chưa được kiểm tra thống nhất. Kafka/FCM vẫn chưa có luồng delivery hoàn chỉnh. Vì vậy lượt tiếp tục này ưu tiên làm chắc luồng quản trị và hộp thông báo trước khi bổ sung xử lý bất đồng bộ.

### Kết quả đã triển khai

- Admin duyệt/từ chối/tạm ngưng listing; xử lý booking case và report; ẩn review; khóa/mở khóa tài khoản. Bổ sung validation lý do/ghi chú và kiểm soát trạng thái, không cho tự khóa mình hoặc hồi sinh tài khoản DELETED. Listing SUSPENDED không thể được host tự submit lại.
- Duyệt tin lưu snapshot terms_version, thời điểm công bố và hạn 30 ngày. Optimistic locking chỉ cho một lần duyệt đồng thời commit. Thay đổi nghiệp vụ, audit và các thông báo liên quan cùng transaction; rollback không để lại thông báo thành công.
- Access JWT mang role thực trong database. Request có JWT kiểm tra trạng thái tài khoản hiện tại; API admin kiểm tra lại role trong database trước cả idempotency replay. Cả status SUSPENDED và cờ suspended legacy đều chặn truy cập. Khóa tài khoản thu hồi refresh session; mở khóa không phục hồi session đã thu hồi.
- Hộp thông báo có phân trang giới hạn 50, thứ tự ổn định created_at/id giảm dần, đếm chưa đọc, đánh dấu một/tất cả đã đọc. Truy cập thông báo của người khác trả 404. POST `/notifications/read-all` là đường dẫn chuẩn; giữ alias `/mark-all-read` để tương thích.
- Admin/Notification dùng DTO trực tiếp hoặc body rỗng đúng controller, lỗi ProblemDTO; POST cần Idempotency-Key. OpenAPI v1.3, contract, changelog và hướng dẫn [ADMIN_NOTIFICATIONS.md](../backend/ADMIN_NOTIFICATIONS.md) đã đồng bộ phạm vi này. Các API cũ ngoài chat/admin/notification vẫn cần một lượt đối chiếu response riêng.

### Bằng chứng kiểm chứng

Lượt Maven `verify` ngày 28/09/2026 đạt **74 test, 0 thất bại, 0 lỗi, 0 bỏ qua**, tăng 19 test so với mốc Phase 7. Ba bộ test mới: `AdminWorkflowTest` (10), `AdminNotificationHttpTest` (6), `AdminAccountAuthTest` (3). Chúng kiểm tra trạng thái, quyền, ownership, rollback, duyệt đồng thời, audit/thông báo, khóa tài khoản/refresh session, role JWT, validation và idempotency trước/sau thay đổi quyền. Báo cáo nằm trong `backend/target/surefire-reports`; log tại `backend/target/phase8-verify.log`.

Lệnh chạy từ `backend`: `.\apache-maven-3.9.9\bin\mvn.cmd -q -l target/phase8-verify.log verify`, với `HOMELY_TEST_MYSQL_URL` trỏ schema test riêng. Flyway đã validate **13 migration V1–V13 trên MySQL 8.0.45**; lượt này không thêm migration. Backend được đóng gói thành `backend/target/rental-0.0.1-SNAPSHOT.jar`.

Ngày 29/09/2026, bản JAR đã khởi động thành công trên instance MySQL test riêng. `/actuator/health` trả 200; request không token tới hộp thông báo và đường dẫn admin trả 401. Log khởi động tại `backend/target/phase8-smoke.log`. Đây là kiểm tra khởi động và bảo vệ HTTP cơ bản, không thay thế nghiệm thu hành trình quản trị từ Android. OpenAPI parse thành công với **23 path, 78 tham chiếu nội bộ được phân giải**, giữ phần `x-stomp` của chat.

### Đánh giá và phần còn lại

Luồng quản trị hiện có bằng chứng kiểm thử cho các ranh giới quan trọng: không duyệt sai trạng thái, không replay kết quả khi đã mất quyền, không tạo audit/thông báo khi giao dịch thất bại và không đọc thông báo của tài khoản khác. Đây là phần backend có thể dùng để tiếp tục tích hợp client. Chưa có cơ sở ghi toàn bộ Phase 8 là hoàn thành; trạng thái trong `schedule.md` đã được sửa tương ứng.

Các giới hạn cần giữ rõ khi tích hợp:

- Xử lý booking case hiện chỉ ghi nhận quyết định và gửi thông báo; chưa thực hiện refund/settlement. Phải chốt quy tắc tài chính và kiểm thử riêng trước khi UI tuyên bố đã hoàn tiền.
- Chưa có endpoint khôi phục listing SUSPENDED. Access JWT vẫn dựa vào TTL và trạng thái tài khoản; sau khi mở khóa, access token cũ còn hạn có thể được chấp nhận, dù refresh session cũ đã bị thu hồi.
- Outbox, Kafka consumer, FCM delivery/retry và việc nối đầy đủ sự kiện booking/payment/viewing vào thông báo còn thiếu. Scheduled jobs cũ chưa được nghiệm thu toàn diện trong lượt này.
- Test giao dịch/concurrency Admin chạy trên H2. MySQL 8.0.45 được dùng cho migration và khởi động ứng dụng; chưa kiểm chứng concurrency Admin trên MySQL hoặc môi trường đích MySQL 8.4.
- Android và E2E xuyên suốt sản phẩm chưa được hoàn thành bởi lượt sửa backend này.

Bước tiếp theo nên hoàn thiện đường đi sự kiện qua outbox/Kafka và kênh push có retry, đồng thời tích hợp hộp thông báo trên Android. Các hành vi tài chính chưa rõ cần được chốt bằng contract và test trước khi nối vào quyết định của Admin.

## 10. Rà soát tổng thể phục vụ demo/bảo vệ — 01/10/2026

### Phạm vi và hướng sản phẩm cập nhật

Người dùng đánh giá Kafka không phù hợp với giai đoạn hiện tại, xác định FCM là bắt buộc và không ưu tiên tính năng lưu nhu cầu tìm phòng. Hướng đề xuất là Spring Boot + MySQL outbox + worker gửi FCM; chưa triển khai thay đổi kiến trúc trong lượt đánh giá này. Các đề xuất Kafka ở mục 9 là lịch sử trước trao đổi này, không phải hướng ưu tiên mới.

Đây là rà soát mã nguồn và đối chiếu tài liệu trong workspace. Các tình huống lỗi dưới đây được suy ra từ luồng controller/service/security hiện có, chưa chạy tái hiện bằng HTTP trong lượt này. 74 test là kết quả lần verify đã ghi ở mục 9, không phải một lần chạy test mới ngày 01/10. Chưa có rubric môn học hoặc mã Android ở repository khác, nên không quy đổi nhận định thành điểm số hay khẳng định mức trừ điểm cụ thể.

### Nhận định chính

Dự án có nền thiết kế đáng giữ: phân module theo nghiệp vụ, tách Room và Listing, dùng Flyway, có snapshot/historical state, và phần chat/admin đã có kiểm thử quyền, transaction, rollback và concurrency. Tuy nhiên mức hoàn thiện giữa các module không đồng đều. Backend có nhiều endpoint nhưng các đoạn nối booking–payment–refund–case còn thiếu; Android trong workspace chưa tạo thành sản phẩm chạy được. Đây là rủi ro lớn khi giảng viên yêu cầu thao tác thực tế hoặc thay đổi tình huống ngoài happy path.

### Những điểm cần khắc phục, theo mức ảnh hưởng

| Ưu tiên | Phát hiện và bằng chứng | Tình huống dễ bị hỏi hoặc lỗi demo | Hướng sửa |
| --- | --- | --- | --- |
| P0 nếu chấm ứng dụng Mobile | `Homely-android/app/src/main/res/layout/activity_main.xml:13` vẫn là Hello World; MainActivity chỉ cài layout/insets; chưa có màn nghiệp vụ hay client API trong source hiện tại. | Yêu cầu đăng nhập, tìm phòng, đặt lịch hoặc nhận push trên điện thoại chưa thực hiện được bằng app này. | Hoàn thiện một hành trình Android nối API thật, có loading/empty/error, khôi phục đăng nhập và xử lý mất mạng. |
| P0 | `BookingService.java:178` chuyển APPROVED → CONFIRMED qua confirmDeposit; dòng 185 vẫn TODO kiểm tra Payment SUCCEEDED. | Tenant gọi xác nhận cọc khi chưa thanh toán vẫn được đổi trạng thái theo luồng hiện tại. | Bắt buộc đối chiếu payment hợp lệ, được phân bổ đúng booking và hạn giữ phòng; thống nhất một đường cập nhật trạng thái. |
| P0 | `MediaController.java:47` nhận roomId rồi gọi `MediaService.attachToResource`; `MediaService.java:79` chỉ kiểm tra uploader của ảnh, không tải/kiểm tra chủ phòng đích. | Người dùng A có thể yêu cầu gắn ảnh do mình upload vào roomId của B; service không có bước từ chối theo ownership phòng. | Kiểm tra phòng tồn tại, chủ phòng, role và purpose trước khi attach. Public đọc ảnh tại dòng 128 cũng cần chính sách visibility phù hợp với listing. |
| P0/P1 | `SecurityConfig.java:110` cho public POST webhook; `PaymentService.java:95` chỉ có comment verify signature. Controller bắt mọi exception và trả 200 ở dòng 29–34. | Người biết mã payment có thể gửi trạng thái thành công vào endpoint mock; khi xử lý lỗi, caller vẫn nhận thành công và không biết cần retry. | Cô lập mock endpoint bằng cấu hình môi trường và cơ chế xác thực phù hợp; tách lỗi vĩnh viễn/lỗi tạm thời, không nuốt mọi lỗi. Việc dùng payment giả lập tự nó phù hợp phạm vi v1. |
| P1 | `PaymentService.java:106` đặt SUCCEEDED nhưng chỉ cập nhật booking nếu đang APPROVED; không có nhánh refund khi booking EXPIRED/CANCELLED. `RefundService.processRefund` chưa có caller trong main source. `BookingService.java:326` chỉ log việc bàn giao quá hạn. | Thanh toán đến sau hết hạn không sinh refund; quá hạn bàn giao không tự mở case. Có service nhưng chưa có luồng hoàn chỉnh. | Nối late settlement/refund/case, ghi history đầy đủ, xử lý trùng và cạnh tranh với expiration job. |
| P1 | `ListingService.java:174` cho thay phí sau khi kiểm tra chủ tin nhưng không kiểm tra trạng thái/booking đang giữ phòng. `BookingService.buildTermsSnapshot` dòng 358 chưa lưu danh sách phí. | Chủ thay phí sau khi khách đặt/giữ phòng; khó chứng minh khách đã đồng ý bộ phí nào. | Chốt quy tắc sửa phí, version và snapshot cả các khoản phí/điều kiện có ảnh hưởng đến quyết định thuê. |
| P1 | `ListingController.java:38` chỉ nhận query và Pageable; repository tìm text bằng LIKE. Contract BR-03 còn yêu cầu giá, diện tích, loại phòng, tiện ích, địa bàn và geo. | Với đề tài tìm phòng, không lọc được theo ngân sách/khu vực là thiếu chức năng cốt lõi dễ thấy khi demo. | Làm trước lọc giá, khu vực, loại phòng và sort hợp lệ; chỉ cam kết geo khi có implementation và kiểm chứng. |
| P1 | FCM vẫn TODO tại `NotificationService.java:80`; Android chưa có Firebase Messaging trong source/dependency hiện tại. | Backend có inbox nhưng điện thoại không nhận được push ngoài app. | Hoàn thiện outbox/worker/FCM và phía Android, quản lý thiết bị/token/logout, mở đúng màn hình từ push và kiểm thử trên thiết bị. Không thêm tìm kiếm đã lưu vào phạm vi ưu tiên. |
| P1 | `FormatRestResponse.java:19` loại chat/admin/notification khỏi wrapper, còn module khác trả wrapper. `IdempotencyFilter.java:49` không áp dụng cho bookings/payments dù contract yêu cầu BOOK01 có Idempotency-Key. | Android deserialize không thống nhất; retry cùng request booking không có cơ chế replay chung như tài liệu mô tả. | Chốt response/pagination/money thống nhất; đồng bộ OpenAPI và mở rộng idempotency cho mutation cần thiết, có test retry và đổi payload. |
| P1 | Test hiện có tập trung chat/admin/auth khóa tài khoản/common. Chưa có bộ test riêng cho BookingService, PaymentService, MediaService, search và viewing. Android chỉ có test mẫu. | 74 test xanh chưa chứng minh hai khách cùng giữ phòng, webhook muộn/trùng, gắn ảnh sai quyền hoặc hành trình Mobile đúng. | Bổ sung test theo rủi ro nghiệp vụ; nghiệm thu concurrency booking/payment với MySQL và một hành trình E2E Android. |
| P1/P2 | `schedule.md:273–274` vẫn ghi P5/P6 hoàn thành. TEST_PLAN dùng thời hạn pending 48h/hold 24h trong khi code dùng 24h/48h. OpenAPI gốc vẫn dùng `/v1`, một số phần mới override `/api/v1`. | Tài liệu, thuyết trình và kết quả thao tác không khớp; khó giải thích điều kiện chuyển trạng thái một cách nhất quán. | Rà lại ma trận yêu cầu–API–test–màn hình, cập nhật trạng thái hoàn thành theo bằng chứng và loại Kafka khỏi kế hoạch khi chốt hướng mới. |
| P2 | `Infra` đang trống, chưa tìm thấy Docker Compose/Dockerfile hoặc bộ kịch bản demo tự động trong workspace. | Đổi máy, khởi tạo database/storage mới hoặc khôi phục dữ liệu demo tốn thao tác thủ công. | Cung cấp cấu hình chạy tái lập, env mẫu không chứa secret, seed cho các vai trò và hướng dẫn/demo script rõ ràng. |

Một khoảng trống bổ sung: `AccountDeletionService.java:53` chưa kiểm tra booking/payment/refund/case đang mở trước khi nhận yêu cầu xóa. Hiện chỉ ghi yêu cầu PENDING, chưa có bằng chứng dữ liệu tài khoản bị xóa ngay; cần trình bày đúng phạm vi này và hoàn thiện điều kiện BLOCKED trước khi nghiệm thu luồng xóa tài khoản.

### Thứ tự sửa để chuẩn bị bảo vệ

1. Khóa các lỗi nghiệp vụ/phân quyền rõ ràng: xác nhận cọc không thanh toán, gắn ảnh sai phòng, webhook mock không được cô lập, late settlement chưa refund. Viết test cho tình huống trước/sau sửa.
2. Chốt một luồng chạy được từ Android: đăng nhập → xem/lọc phòng → tạo booking → chủ duyệt → thanh toán sandbox → xác nhận đúng trạng thái. Có dữ liệu và tài khoản demo để lặp lại.
3. Hoàn thiện FCM cho các sự kiện đã tồn tại như tin nhắn, booking và lịch xem; tích hợp inbox và điều hướng từ notification trên Android.
4. Đồng bộ API/tài liệu; thêm bài test timeout, retry, concurrency và khởi tạo môi trường mới. Trình bày rõ phần nào đã nghiệm thu và phần nào nằm ngoài phạm vi.

Những câu nên tự kiểm tra trước buổi bảo vệ: “Chưa trả tiền có xác nhận cọc được không?”, “Tài khoản A sửa ảnh phòng B có bị chặn không?”, “Hai khách được duyệt cùng lúc thì ai giữ được phòng?”, “Tiền đến sau hết hạn được xử lý thế nào?”, “Mất mạng rồi bấm lại có tạo thao tác trùng không?”, “Ứng dụng đang ở nền có nhận và mở đúng thông báo không?”. Các câu này kiểm tra trực tiếp tính đúng của sản phẩm, không cần bổ sung công nghệ hay tính năng mới để chứng minh.
## 11. Khắc phục theo yêu cầu — 03/10/2026

Người dùng yêu cầu xử lý toàn bộ điểm yếu ở mục 10 và xác nhận chưa có Firebase project: chuẩn bị code/hướng dẫn trước. Đã triển khai các đường nối booking–payment–refund–case, media ownership, search, outbox FCM, Android, SMTP và cấu hình demo. Bảng hành vi và bằng chứng cập nhật tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md); hướng dẫn chạy ở [Infra/README.md](../Infra/README.md).

Chưa kết luận backend hoàn chỉnh: đang kiểm tra MySQL thật và HTTP workflow, đồng bộ OpenAPI; chưa có kết quả push thật/Android E2E. Lượt verify trước kiểm tra MySQL đạt 90 test, 1 test bỏ qua; Android đã build/lint thành công. Các kết quả cuối sẽ được ghi sau khi chạy, không suy ra từ việc có code.
