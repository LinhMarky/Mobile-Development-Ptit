package com.homely.student14.dto.request.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateConversationDTO {
    @NotNull(message = "Bạn cần chọn phòng.")
    @Positive(message = "Mã phòng không hợp lệ.")
    private Long roomId;
}
