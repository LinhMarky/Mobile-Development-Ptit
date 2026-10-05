# ROOMLY — Hướng dẫn chạy Frontend Android

Frontend của ứng dụng tìm phòng trọ ROOMLY được xây dựng bằng Java và XML.

Project Android nằm trong thư mục `Frontend-Mobile-App`.

## 1. Công nghệ và thư viện

- Java, XML Layout.
- ViewBinding.
- AndroidX Fragment.
- RecyclerView.
- Material Components.
- AppCompat.
- ConstraintLayout.
- AndroidX Core và Activity.

Các thư viện đã được khai báo trong project. Gradle tự tải khi Sync, không cần cài từng thư viện thủ công.

## 2. Môi trường cần chuẩn bị

- Android Studio.
- Android SDK Platform 37, theo cấu hình hiện tại của project.
- Máy ảo hoặc điện thoại Android API 24 trở lên.
- Internet để tải SDK và các thư viện trong lần thiết lập đầu tiên.

Cấu hình hiện tại:

| Thành phần                       | Cấu hình   |
| -------------------------------- | ---------- |
| Ngôn ngữ giao diện               | Java + XML |
| Android Gradle Plugin            | 9.3.3      |
| compileSdk                       | 37         |
| targetSdk                        | 37         |
| minSdk                           | 24         |
| Java source/target compatibility | 11         |

Java source/target 11 là mức tương thích của code, không phải yêu cầu dùng JDK 11 để chạy Gradle. Khi thiết lập Gradle JDK trong Android Studio, dùng JDK tương thích với phiên bản Gradle/AGP của project.

Sử dụng Gradle Wrapper đã có trong repository, không cần cài Gradle riêng.

## 3. Lấy code

### Nếu chưa clone repository

Trên GitHub, mở repository của nhóm, chọn **Code → HTTPS** và sao chép URL.

Chạy lệnh sau, thay `URL_REPO_NHOM` bằng URL vừa sao chép:

```bash
git clone --branch dev URL_REPO_NHOM
```

### Nếu đã có repository trên máy

Mở terminal tại thư mục gốc repository:

```bash
git status
git fetch origin
git switch dev
git pull origin dev
```

Nếu chưa có nhánh `dev` trên máy, dùng:

```bash
git switch --track origin/dev
```

Commit hoặc stash các thay đổi đang làm trước khi chuyển nhánh hoặc pull.

## 4. Mở bằng Android Studio

1. Mở Android Studio → chọn **Open**.
2. Chọn thư mục `Frontend-Mobile-App` bên trong repository.
3. Đảm bảo thư mục được chọn chứa:
   - `app/`
   - `gradle/`
   - `settings.gradle.kts`
   - `build.gradle.kts`
   - `gradlew`
   - `gradlew.bat`
4. Chờ Gradle Sync và lập chỉ mục hoàn tất.
5. Nếu thiếu SDK, cài các thành phần được yêu cầu trong SDK Manager.
6. Nếu có lỗi Sync, kiểm tra nội dung lỗi trước khi chạy app.

Không mở riêng thư mục `app`. Không dùng thư mục gốc repository làm project Android.

## 5. Chạy ứng dụng

### Máy ảo Android

1. Mở **Device Manager**.
2. Tạo máy ảo và tải system image phù hợp với máy tính.
3. Chọn phiên bản Android API 24 trở lên.
4. Khởi động máy ảo và chờ thiết bị vào màn hình chính.
5. Chọn cấu hình chạy `app` và thiết bị vừa khởi động.
6. Bấm **Run ▶**.

### Điện thoại thật

1. Bật **Developer options** trên điện thoại.
2. Bật **USB debugging**.
3. Kết nối điện thoại với máy tính qua USB.
4. Chấp nhận thông báo cho phép USB debugging.
5. Chọn điện thoại trong Android Studio.
6. Bấm **Run ▶**.

## 6. Build APK bằng terminal

Các lệnh dưới đây bắt đầu từ thư mục gốc repository.

### Windows PowerShell

```powershell
cd Frontend-Mobile-App
.\gradlew.bat assembleDebug
```

### macOS/Linux

```bash
cd Frontend-Mobile-App
./gradlew assembleDebug
```

Nếu báo không có quyền thực thi:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

APK được tạo tại:

```text
Frontend-Mobile-App/app/build/outputs/apk/debug/app-debug.apk
```

Nếu terminal đã ở trong `Frontend-Mobile-App`, bỏ qua lệnh `cd`.

Build bằng terminal cần JDK tương thích với Gradle/AGP. Nếu build trong Android Studio thành công nhưng terminal thất bại, kiểm tra JDK mà terminal sử dụng.

## 7. Chức năng hiện có

| Màn hình       | Chức năng                                  |
| -------------- | ------------------------------------------ |
| Khám phá       | Danh sách phòng, tìm kiếm theo tên/địa chỉ |
| Bộ lọc         | Lọc loại phòng, khoảng giá và diện tích    |
| Chi tiết phòng | Hiển thị thông tin phòng được chọn         |
| Đã lưu         | Lưu/bỏ lưu và xem danh sách phòng đã lưu   |
| Đặt lịch       | Chọn ngày, giờ và nhập ghi chú             |
| Lịch trình     | Xem và hủy lịch hẹn mẫu                    |
| Cá nhân        | Xem hồ sơ, sửa tên và số điện thoại        |

Phần Tin nhắn đang được xây dựng. Layout danh sách đã có, nhưng chưa hoàn thành chức năng trò chuyện và chưa nối tab.

Giao diện admin và kết nối backend chưa hoàn thành.

## 8. Dữ liệu demo

Ứng dụng hiện dùng dữ liệu mẫu để kiểm tra giao diện:

- Phòng và ảnh mẫu có sẵn trong project.
- Các repository mẫu giữ dữ liệu trong bộ nhớ.
- Có thể chuyển tab mà vẫn giữ thông tin đã sửa trong cùng tiến trình app.
- Khi tiến trình app bị đóng, dữ liệu có thể trở về mặc định.
- Đặt lịch chưa gửi yêu cầu đến chủ phòng.
- Email hồ sơ hiện chỉ đọc.
- Chưa cần API URL hoặc tài khoản backend để chạy demo.

## 9. Cấu trúc thư mục frontend

Các đường dẫn dưới đây tính từ `Frontend-Mobile-App`.

| Đường dẫn                                                | Nội dung                       |
| -------------------------------------------------------- | ------------------------------ |
| `app/src/main/java/com/example/roomly/MainActivity.java` | Màn hình chính và điều hướng   |
| `app/src/main/java/com/example/roomly/data/model/`       | Các lớp dữ liệu                |
| `app/src/main/java/com/example/roomly/data/repository/`  | Repository dữ liệu mẫu         |
| `app/src/main/java/com/example/roomly/ui/`               | Fragment và Adapter            |
| `app/src/main/res/layout/`                               | Giao diện XML                  |
| `app/src/main/res/drawable/`                             | Ảnh, icon và nền               |
| `app/src/main/res/menu/`                                 | Menu điều hướng                |
| `app/src/main/res/color/`                                | Bộ màu theo trạng thái         |
| `app/src/main/res/values/`                               | Màu, chuỗi và theme            |
| `app/src/main/AndroidManifest.xml`                       | Cấu hình ứng dụng              |
| `gradle/libs.versions.toml`                              | Khai báo phiên bản và thư viện |

Không sửa trực tiếp các file Binding trong thư mục `build`. Chúng được sinh tự động từ XML.

## 10. Làm việc bằng VS Code và Android Studio

Hai công cụ có thể mở cùng một thư mục code:

- VS Code: chỉnh sửa file và thao tác Git.
- Android Studio: Sync Gradle, xem layout, build và chạy app.

Lưu file trước khi chuyển công cụ. Không cần sao chép code giữa hai nơi.

Sau khi pull thay đổi liên quan đến cấu hình Gradle, thực hiện Gradle Sync trong Android Studio.

## 11. Xử lý lỗi thường gặp

### Thiếu Android SDK

Mở SDK Manager, cài SDK theo thông báo lỗi. Project hiện dùng compileSdk 37.

### Không tải được thư viện

Kiểm tra Internet và thông báo lỗi trong cửa sổ Sync/Build, sau đó thực hiện lại Gradle Sync.

### Lớp Binding báo đỏ

1. Kiểm tra Gradle Sync.
2. Kiểm tra lỗi XML và tên file layout.
3. Build project để sinh lớp Binding.
4. Kiểm tra import có đúng package `com.example.roomly.databinding` không.

Không tự tạo lớp Binding bằng tay.

### Import Fragment báo đỏ

Kiểm tra package trong file Java và vị trí thư mục.

Ví dụ:

```java
package com.example.roomly.ui.schedule;
```

File phải nằm trong:

```text
app/src/main/java/com/example/roomly/ui/schedule/
```

### Máy ảo không khởi động hoặc Run quá lâu

Kiểm tra trạng thái thiết bị trong Device Manager và thông báo Run. Có thể thử khởi động lại máy ảo bằng Cold Boot hoặc dùng điện thoại thật.

### Thông tin cần gửi khi báo lỗi

- Nội dung lỗi trong Sync, Build hoặc Logcat.
- Nhánh và commit đang chạy.
- Phiên bản Android Studio.
- Thiết bị và phiên bản Android.

## 12. File cần đưa lên Git

Commit các file nguồn và cấu hình:

- `Frontend-Mobile-App/app/src/`
- Các file cấu hình Gradle của project và module.
- `Frontend-Mobile-App/gradle/`, gồm version catalog và Wrapper.
- `Frontend-Mobile-App/gradlew`
- `Frontend-Mobile-App/gradlew.bat`
- Các file `.gitignore`.
- `README.md`.

Không commit:

- `local.properties`: chứa đường dẫn SDK riêng của từng máy.
- Thư mục `.gradle/`.
- Các thư mục `build/`.
- Mật khẩu, token hoặc thông tin bí mật.
