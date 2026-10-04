package com.homely.rental.media.validation;

import com.homely.rental.common.exception.IdInvalidException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * File validator using magic bytes (BR-09, SEC-05).
 * Validates file type, size, and content type consistency.
 */
@Component
public class FileValidator {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;  // 10 MB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;  // 50 MB

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4"
    );

    // Magic bytes for common file types
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}  // RIFF
    );

    // MP4 magic: bytes 4-7 = "ftyp"
    private static final byte[] FTYP = new byte[]{0x66, 0x74, 0x79, 0x70};

    /**
     * Validate an uploaded file for type, size, and magic bytes.
     *
     * @param file uploaded multipart file
     * @param isVideo whether to validate as video
     * @throws IdInvalidException if validation fails
     */
    public void validate(MultipartFile file, boolean isVideo) throws IdInvalidException {
        if (file == null || file.isEmpty()) {
            throw new IdInvalidException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IdInvalidException("Content type is missing");
        }

        if (isVideo) {
            if (!ALLOWED_VIDEO_TYPES.contains(contentType)) {
                throw new IdInvalidException("Invalid video type. Allowed: " + ALLOWED_VIDEO_TYPES);
            }
            if (file.getSize() > MAX_VIDEO_SIZE) {
                throw new IdInvalidException("Video file too large. Max: 50 MB");
            }
        } else {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
                throw new IdInvalidException("Invalid image type. Allowed: " + ALLOWED_IMAGE_TYPES);
            }
            if (file.getSize() > MAX_IMAGE_SIZE) {
                throw new IdInvalidException("Image file too large. Max: 10 MB");
            }
        }

        // Magic bytes validation
        validateMagicBytes(file, contentType);
    }

    private void validateMagicBytes(MultipartFile file, String contentType) throws IdInvalidException {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read < 4) {
                throw new IdInvalidException("File too small to validate");
            }

            if ("video/mp4".equals(contentType)) {
                // MP4: bytes 4-7 should be "ftyp"
                if (read < 8) throw new IdInvalidException("Invalid MP4 file");
                for (int i = 0; i < FTYP.length; i++) {
                    if (header[4 + i] != FTYP[i]) {
                        throw new IdInvalidException("File content does not match MP4 format");
                    }
                }
                return;
            }

            byte[] expected = MAGIC_BYTES.get(contentType);
            if (expected != null) {
                for (int i = 0; i < expected.length; i++) {
                    if (header[i] != expected[i]) {
                        throw new IdInvalidException(
                                "File content does not match declared type: " + contentType);
                    }
                }
            }
        } catch (IOException e) {
            throw new IdInvalidException("Cannot read file for validation");
        }
    }
}
