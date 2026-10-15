package com.homely.student14.dto.request.chat;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageDTO {
    @NotBlank(message = "Nội dung tin nhắn không được để trống.")
    private String content;

    @NotBlank(message = "Mã tin nhắn không được để trống.")
    private String clientMessageId;
}
