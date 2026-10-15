package com.homely.student14.dto.request.viewing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateViewingDTO {
    @NotNull(message = "Bạn cần chọn phòng.")
    @Positive(message = "Mã phòng không hợp lệ.")
    private Long roomId;

    @NotBlank(message = "Bạn cần chọn giờ bắt đầu.")
    private String startAt;

    @NotBlank(message = "Bạn cần chọn giờ kết thúc.")
    private String endAt;
}
