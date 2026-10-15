package com.homely.student14.dto.request.report;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ResolveReportDTO {
    @NotBlank(message = "Vui lòng nhập ghi chú xử lý.")
    private String resolutionNote;

    private String status;

    // Chỉ áp dụng khi xử lý khiếu nại thuê phòng.
    private String decision;

    // Chỉ bắt buộc khi quyết định là SPLIT.
    private BigDecimal tenantRefundVnd;
}
