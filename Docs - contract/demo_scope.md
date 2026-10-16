# 🎯 BƯỚC 1 — XÁC NHẬN PHẠM VI DEMO ROOMLY

## Kết quả xác minh

| Hạng mục | Xác nhận |
|---|---|
| **FRONTEND** | `Frontend-Mobile-App` — package `com.example.roomly` |
| **API Base URL** | `http://10.0.2.2:18092/api/v1/` (emulator → localhost:18092) |
| **BACKEND MAIN CLASS** | `com.homely.student14.Student14Application` |
| **SPRING PROFILE** | `student14` |
| **PORT** | `18092` |
| **DATABASE** | MySQL `127.0.0.1:3306` / schema `homely_student14` |
| **DB USER** | `homely_student14` |
| **JDBC** | Spring JDBC thuần (JPA/Hibernate/Flyway đều bị exclude) |
| **BACKEND PACKAGE** | `com.homely.student14.*` (controller, service, repository, dto, entity, config, exception) |
| **JAR** | `backend/target/rental-student14.jar` (build bằng `mvn package -Pstudent14`) |
| **Launcher script** | `Infra/student14/run.py` |

## Phạm vi file được phép sửa

```
✅ ĐƯỢC SỬA:
  Frontend-Mobile-App/app/src/main/java/com/example/roomly/**
  Frontend-Mobile-App/app/src/main/res/**
  Frontend-Mobile-App/app/src/test/**
  Frontend-Mobile-App/app/src/androidTest/**
  backend/src/main/java/com/homely/student14/**
  backend/src/main/resources/application-student14.properties
  backend/src/test/java/com/homely/student14/**

❌ KHÔNG ĐƯỢC SỬA:
  Homely-android/
  backend/src/main/java/com/homely/rental/  (package cũ, không phục vụ demo)
  backend/src/main/resources/db/migration/  (Flyway disabled cho student14)
  Docs - contract/
  Infra/ (trừ khi chỉ đọc để tham khảo)
  Bất kỳ file nào khác
```

## Trạng thái Codex đã tạo (untracked files)

Codex đã tạo toàn bộ package `com.homely.student14` (backend JDBC mới) và nhiều file mới trong Frontend:
- **Backend student14**: ~40 file Java (controllers, services, repositories, DTOs, entities)
- **Frontend mới**: ListingFee model, ListingFeeEditor UI, HostSaveViewModel, BookingRepository, AdminRepository, NotificationFragment, BookingFragment, ReportFragment, và nhiều layout XML

**Phụ phí (Listing Fees)** đã được Codex triển khai:
- **Backend**: `CatalogService.validateFees()`, `CatalogRepository` INSERT/UPDATE với `fees` JSON column, `ListingFeeDTO`
- **Frontend**: `ListingFee.java` model, `ListingFeeEditor.java` UI, tích hợp vào `HostCreateListingFragment` và `HostEditListingFragment`

## Backup

- Chưa có git stash. Toàn bộ thay đổi đang ở working tree (337 files modified/untracked).
- Commit cuối: `8188066 Remove unnecessary log files`
- **Cần tạo backup trước khi sửa bất kỳ file nào.**

## Việc tiếp theo (BƯỚC 2)

1. Chờ kết quả Android build → xác nhận lỗi biên dịch còn lại
2. Nếu build PASS → chuyển sang kiểm tra listing fees lifecycle
3. Nếu build FAIL → sửa lỗi biên dịch trước
