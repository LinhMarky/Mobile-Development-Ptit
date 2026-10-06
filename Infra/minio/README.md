# MinIO phục vụ demo

Compose build MinIO từ source chính thức, khóa ở tag `RELEASE.2025-04-22T22-12-26Z`. `go install` tải module qua Go proxy và kiểm tra checksum; không dùng image từ một tác giả không rõ nguồn gốc.

Trong lượt nghiệm thu 05/10/2026, Docker Hub trả `pull access denied` cho image cũ, còn Quay trả 401. Dockerfile này giúp tái dựng môi trường mà không phụ thuộc hai image đó. Build lần đầu tải Go/dependencies nên có thể mất vài phút; những lần sau dùng cache.

- [Source chính thức tại tag đã khóa](https://github.com/minio/minio/tree/RELEASE.2025-04-22T22-12-26Z).
- [Go module của tag này yêu cầu Go 1.24](https://github.com/minio/minio/blob/RELEASE.2025-04-22T22-12-26Z/go.mod).
- [License AGPL-3.0](https://github.com/minio/minio/blob/RELEASE.2025-04-22T22-12-26Z/LICENSE).

Repository community đã được chủ sở hữu archive. Cấu hình này dành cho demo local; khi triển khai thực tế, dùng S3/object storage còn được hỗ trợ. Backend vẫn dùng abstraction `StorageService`, nên thay endpoint/credentials không buộc thay các controller nghiệp vụ.
