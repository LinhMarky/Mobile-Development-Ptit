package com.homely.student14.dto.request.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SaveRoomDTO {
    @Size(max = 30, message = "Mã phòng tối đa 30 ký tự.")
    private String unitCode;

    @NotBlank(message = "Vui lòng nhập tên phòng.")
    @Size(max = 150, message = "Tên phòng tối đa 150 ký tự.")
    private String name;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự.")
    private String description = "";

    @NotBlank(message = "Vui lòng chọn loại phòng.")
    @Pattern(regexp = "SINGLE_ROOM|STUDIO|MINI_APARTMENT", message = "Loại phòng không hợp lệ.")
    private String roomType;

    @NotNull(message = "Vui lòng nhập diện tích phòng.")
    @DecimalMin(value = "2", message = "Diện tích từ 2 đến 1000 m².")
    @DecimalMax(value = "1000", message = "Diện tích từ 2 đến 1000 m².")
    private BigDecimal areaM2;

    @NotNull(message = "Vui lòng nhập số người tối đa.")
    @Min(value = 1, message = "Số người từ 1 đến 10.")
    @Max(value = 10, message = "Số người từ 1 đến 10.")
    private Integer maxOccupants = 1;

    @NotBlank(message = "Vui lòng nhập địa chỉ phòng.")
    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự.")
    private String addressLine;

    @NotNull(message = "Tiện ích cần là danh sách ID.")
    private List<@NotNull @Min(1) @Max(15) Long> amenityIds = new ArrayList<>();
}
