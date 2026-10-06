package com.homely.rental.media;

import com.homely.rental.media.entity.*;
import com.homely.rental.media.service.MediaService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class MediaUrlTest {
    @ParameterizedTest @CsvSource({
            "api/v1, /api/v1/media/7/content",
            "custom/v2, /custom/v2/media/7/content",
            "/custom/v2, /custom/v2/media/7/content",
            "custom/v2/, /custom/v2/media/7/content",
            "/custom/v2/, /custom/v2/media/7/content"})
    void urlUsesNormalizedConfiguredPrefix(String prefix, String expected) {
        MediaService service = new MediaService(null, null, null, null, null, null);
        ReflectionTestUtils.setField(service, "apiPrefix", prefix);
        Media media = new Media(); media.setId(7L); media.setPurpose(MediaPurpose.ROOM_PHOTO); media.setStatus(MediaStatus.READY);
        assertThat(service.toDTO(media).getUrl()).isEqualTo(expected);
    }
}
