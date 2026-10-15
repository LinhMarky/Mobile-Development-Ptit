package com.homely.student14.dto.request.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadImageDTO {
    @NotBlank(message = "Vui lòng chọn ảnh.")
    @Size(max = 5600000, message = "Chọn ảnh JPG/PNG tối đa 4 MB.")
    private String contentBase64;
}
