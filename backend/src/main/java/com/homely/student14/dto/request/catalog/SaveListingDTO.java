package com.homely.student14.dto.request.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaveListingDTO {
    @NotBlank(message = "Vui lòng nhập tiêu đề tin đăng.")
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự.")
    private String title;

    @NotBlank(message = "Vui lòng nhập mô tả tin đăng.")
    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự.")
    private String description;

    @NotNull(message = "Vui lòng nhập giá thuê.")
    @Positive(message = "Giá thuê phải dương.")
    private BigDecimal rentVnd;

    @NotNull(message = "Vui lòng nhập tiền cọc.")
    @PositiveOrZero(message = "Tiền cọc không được âm.")
    private BigDecimal depositVnd = BigDecimal.ZERO;

    @Valid
    @NotNull(message = "Phụ phí cần là danh sách.")
    @Size(max = 12, message = "Tối đa 12 khoản phụ phí.")
    private List<@NotNull ListingFeeDTO> fees = new ArrayList<>();
}
