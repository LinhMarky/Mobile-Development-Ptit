# ROOM RENTAL SYSTEM — TECHNICAL DOCUMENTATION

Chào mừng bạn đến với bộ tài liệu kỹ thuật của dự án **Hệ thống tìm kiếm, hẹn xem, giữ phòng và đánh giá phòng trọ**.

> **Contract v1.6 — 05/10/2026:** Hoàn thiện auth, xóa tài khoản, retention và vận hành theo [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md). HTTP JSON tiếp tục dùng global wrapper `{statusCode,message,data}`. [PROJECT_CONTRACT.md](PROJECT_CONTRACT.md) là giao thức hiện tại; [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md) lưu bằng chứng chuyển đổi v1.5.

Để tránh trùng lặp và đảm bảo tính nhất quán cao nhất, hệ thống tài liệu được thiết kế tối giản với duy nhất **MỘT Source of Truth** cho mọi business rules và protocol.

---

## 1. Kiến trúc tài liệu

Tất cả tài liệu kỹ thuật nằm trong thư mục `/docs`:

| File | Vai trò | Trách nhiệm chính |
|---|---|---|
| **PROJECT_CONTRACT.md** | **Business Source of Truth** | Định nghĩa API, DTO, Enum, Business Rules, Error, Pagination, File Upload. |
| **openapi.yaml** | **Machine API Contract** | Bản đặc tả API chuẩn OpenAPI 3.1, khớp tuyệt đối với PROJECT_CONTRACT. |
| **FE_SPEC.md** | **Frontend Spec** | Kiến trúc Android, User Flows, Navigation, State Matrix, API Mapping. |
| **BE_SPEC.md** | **Backend Spec** | Kiến trúc Spring Boot, Module, Domain Responsibility, Event, Job. |
| **TEST_PLAN.md** | **QA Spec** | Acceptance Criteria, E2E Scenarios, State Transition & Concurrency Tests. |
| **CONTRACT_GAPS.md** | **Working File** | Ghi nhận các khoảng trống, mâu thuẫn chờ xác nhận. |
| **CHANGELOG.md** | **Version Control** | Lịch sử thay đổi hợp đồng giao thức sau khi bắt đầu code. |

---

## 2. Hướng dẫn đọc cho từng vai trò

Không yêu cầu ai phải đọc toàn bộ mọi tài liệu. Tùy theo vai trò, hãy theo luồng đọc sau:

### 📱 Nếu bạn là Frontend Developer (Android)
1. Đọc `00_README.md` (file này).
2. Đọc `FE_SPEC.md` để hiểu kiến trúc UI, các luồng màn hình, cách quản lý state và mapping API.
3. Khi cần biết chính xác business rule, tham chiếu chéo sang `PROJECT_CONTRACT.md`.
4. Dùng `openapi.yaml` để sinh code hoặc kiểm tra cấu trúc API.

### ⚙️ Nếu bạn là Backend Developer (Spring Boot)
1. Đọc `00_README.md` (file này).
2. Đọc `BE_SPEC.md` để hiểu cách chia module, phân quyền, database mapping, transaction, concurrency.
3. Khi implement một chức năng, tham chiếu quy tắc bắt buộc tại `PROJECT_CONTRACT.md`.
4. Viết code API tuân thủ đúng `openapi.yaml`.

### 🧪 Nếu bạn là QA / Tester
1. Đọc `00_README.md` (file này).
2. Đọc `TEST_PLAN.md` để lấy danh sách Acceptance Criteria và các kịch bản E2E.
3. Kiểm chứng các ngoại lệ nghiệp vụ và State Machine dựa vào `PROJECT_CONTRACT.md`.

---

## 3. Quy tắc chống trùng lặp (Anti-Duplication Rules)

- **Một thông tin chỉ có MỘT Source of Truth.**
- Business rules của hệ thống (ví dụ: *Quy tắc giữ chỗ khi Booking được duyệt*) **chỉ** nằm trong `PROJECT_CONTRACT.md`. FE_SPEC hay BE_SPEC chỉ được *tham chiếu* (reference) đến mã rule (ví dụ: `Source: PROJECT_CONTRACT BR-06`), tuyệt đối không diễn đạt lại bằng lời văn khác.
- Dữ liệu API request/response chỉ nằm trong `PROJECT_CONTRACT.md` và `openapi.yaml`.

*Bất cứ khi nào bạn phát hiện mâu thuẫn giữa code và contract, hãy ghi nhận vào `CONTRACT_GAPS.md` để team cùng review, không tự ý giải quyết cục bộ.*
> **Cập nhật 03/10/2026:** Hiện trạng triển khai và các quyết định thay thế mô tả cũ nằm tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). FCM dùng MySQL outbox; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các mốc kiểm chứng trước đây là lịch sử, không phải nghiệm thu bản hiện tại.
# Hiện trạng triển khai

Xem [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) để biết phần đã kiểm chứng và giới hạn còn lại. Chạy demo theo [Infra/README.md](../Infra/README.md); cấu hình push theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
