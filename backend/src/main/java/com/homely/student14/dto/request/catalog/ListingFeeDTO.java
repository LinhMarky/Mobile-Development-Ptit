package com.homely.student14.dto.request.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ListingFeeDTO {
    @NotBlank(message = "Vui lòng chọn loại phụ phí.")
    @Pattern(
            regexp = "ELECTRICITY|WATER|INTERNET|PARKING|CLEANING|SERVICE|OTHER",
            message = "Mã phụ phí không hợp lệ.")
    private String feeCode;

    @NotBlank(message = "Vui lòng chọn cách tính phụ phí.")
    @Pattern(regexp = "FIXED|PER_UNIT", message = "Cách tính phụ phí không hợp lệ.")
    private String feeMode;

    @Size(max = 50, message = "Đơn vị phụ phí tối đa 50 ký tự.")
    private String unitName = "";

    @NotNull(message = "Phụ phí cần có số tiền.")
    @PositiveOrZero(message = "Phụ phí không được âm.")
    private BigDecimal amountVnd;
}
