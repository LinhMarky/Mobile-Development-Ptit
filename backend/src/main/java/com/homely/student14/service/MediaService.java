package com.homely.student14.service;

import static com.homely.student14.repository.DatabaseRepository.fail;
import static com.homely.student14.repository.DatabaseRepository.flag;
import static com.homely.student14.repository.DatabaseRepository.id;

import com.homely.student14.dto.request.media.UploadImageDTO;
import com.homely.student14.repository.MediaRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
public class MediaService {
    private static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 16_000_000;

    private final MediaRepository mediaRepository;
    private final CatalogService catalogService;
    private final AuthService authService;
    private final Path storageDirectory;

    public MediaService(
            MediaRepository mediaRepository,
            CatalogService catalogService,
            AuthService authService,
            @Value("${student14.media-directory}") String mediaDirectory) {
        this.mediaRepository = mediaRepository;
        this.catalogService = catalogService;
        this.authService = authService;
        this.storageDirectory = Path.of(mediaDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public Map<String, Object> upload(long roomId, Jwt jwt, UploadImageDTO request) {
        this.catalogService.ownedRoom(roomId, jwt, true);
        byte[] imageBytes = decodeImage(request.getContentBase64());
        Path imageFile = this.storageDirectory.resolve(UUID.randomUUID() + ".jpg");

        try {
            BufferedImage originalImage = readSupportedImage(imageBytes);
            BufferedImage jpegImage = convertToJpeg(originalImage);
            saveImageFile(jpegImage, imageFile);

            Map<String, Object> currentUser = this.authService.user(jwt);
            long uploaderId = id(currentUser.get("id"));
            this.mediaRepository.markAttachedRoomImagesDeleted(roomId);

            long imageId =
                    this.mediaRepository.createRoomImage(
                            uploaderId,
                            roomId,
                            Files.size(imageFile),
                            imageFile.getFileName().toString(),
                            jpegImage.getWidth(),
                            jpegImage.getHeight());

            return Map.of("id", imageId, "url", "/api/v1/media/" + imageId + "/content");
        } catch (IOException exception) {
            deleteFailedUpload(imageFile);
            throw fail(400, "Không đọc hoặc lưu được ảnh.");
        } catch (RuntimeException exception) {
            deleteFailedUpload(imageFile);
            throw exception;
        }
    }

    private byte[] decodeImage(String contentBase64) {
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(contentBase64.trim());
        } catch (IllegalArgumentException exception) {
            throw fail(400, "Ảnh không đúng định dạng.");
        }

        if (imageBytes.length == 0 || imageBytes.length > MAX_IMAGE_BYTES) {
            throw fail(400, "Chọn ảnh JPG/PNG tối đa 4 MB.");
        }

        return imageBytes;
    }

    private BufferedImage readSupportedImage(byte[] imageBytes) throws IOException {
        try (ImageInputStream input =
                ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
            if (input == null) {
                throw fail(400, "Không đọc được ảnh.");
            }

            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(input);
            if (!imageReaders.hasNext()) {
                throw fail(400, "Chọn ảnh JPG hoặc PNG.");
            }

            ImageReader imageReader = imageReaders.next();
            try {
                imageReader.setInput(input);
                String format = imageReader.getFormatName();
                if (!"JPEG".equalsIgnoreCase(format) && !"PNG".equalsIgnoreCase(format)) {
                    throw fail(400, "Chỉ nhận ảnh JPG/PNG.");
                }

                int width = imageReader.getWidth(0);
                int height = imageReader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw fail(400, "Ảnh có độ phân giải quá lớn.");
                }

                BufferedImage image = imageReader.read(0);
                if (image == null) {
                    throw fail(400, "Không đọc được ảnh.");
                }

                return image;
            } finally {
                imageReader.dispose();
            }
        }
    }

    // Chuẩn hóa về JPEG; nền trong suốt của PNG được đổi thành màu trắng.
    private BufferedImage convertToJpeg(BufferedImage originalImage) {
        BufferedImage jpegImage =
                new BufferedImage(
                        originalImage.getWidth(),
                        originalImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = jpegImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, jpegImage.getWidth(), jpegImage.getHeight());
            graphics.drawImage(originalImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return jpegImage;
    }

    private void saveImageFile(BufferedImage image, Path imageFile) throws IOException {
        Files.createDirectories(this.storageDirectory);
        boolean saved = ImageIO.write(image, "jpg", imageFile.toFile());
        if (!saved) {
            throw fail(400, "Không xử lý được ảnh.");
        }
    }

    @Transactional
    public void remove(long roomId, Jwt jwt) {
        this.catalogService.ownedRoom(roomId, jwt, true);
        this.mediaRepository.markRoomImagesDeleted(roomId);
    }

    public Path content(long imageId, Jwt jwt) {
        Map<String, Object> image = this.mediaRepository.findAttachedImageById(imageId);
        if (!isPublicImage(image)) {
            Map<String, Object> currentUser = this.authService.user(jwt);
            boolean isOwner = id(currentUser.get("id")) == id(image.get("host_id"));
            boolean isAdmin = flag(currentUser.get("is_admin"));
            if (!isOwner && !isAdmin) {
                throw fail(403, "Bạn không được xem ảnh này.");
            }
        }

        return findStoredFile(image.get("storage_key").toString());
    }

    private boolean isPublicImage(Map<String, Object> image) {
        if (!"PUBLISHED".equals(image.get("listing_status"))) {
            return false;
        }

        Object expiresAt = image.get("expires_at");
        return expiresAt == null || Instant.parse(expiresAt.toString()).isAfter(Instant.now());
    }

    private Path findStoredFile(String storageKey) {
        if (!storageKey.matches("[0-9a-f-]{36}\\.jpg")) {
            throw fail(404, "Không tìm thấy ảnh.");
        }

        Path imageFile = this.storageDirectory.resolve(storageKey).normalize();
        if (!imageFile.startsWith(this.storageDirectory) || !Files.isRegularFile(imageFile)) {
            throw fail(404, "Không tìm thấy ảnh.");
        }

        return imageFile;
    }

    private void deleteFailedUpload(Path imageFile) {
        try {
            Files.deleteIfExists(imageFile);
        } catch (IOException ignored) {
            // Lỗi xóa tệp không che mất lỗi tải ảnh ban đầu.
        }
    }
}
