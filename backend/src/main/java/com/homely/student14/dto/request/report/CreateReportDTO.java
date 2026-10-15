package com.homely.student14.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportDTO {
    @NotBlank(message = "Vui lòng chọn đối tượng cần báo cáo.")
    private String targetType;

    @NotNull(message = "Thiếu mã đối tượng cần báo cáo.")
    @Positive(message = "Mã đối tượng cần báo cáo không hợp lệ.")
    private Long targetId;

    @NotBlank(message = "Vui lòng chọn lý do báo cáo.")
    private String reasonCode;

    // Báo cáo tin đăng có thể bỏ trống; khiếu nại thuê phòng cần ít nhất 10 ký tự.
    private String description;
}
