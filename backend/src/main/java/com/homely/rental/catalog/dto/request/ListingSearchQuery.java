package com.homely.rental.catalog.dto.request;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class ListingSearchQuery {
    @Size(max=200) private String query;
    private String room_type;
    @DecimalMin("0") @Digits(integer=18,fraction=0) private BigDecimal min_rent_vnd;
    @DecimalMin("0") @Digits(integer=18,fraction=0) private BigDecimal max_rent_vnd;
    @DecimalMin("0") private BigDecimal min_area_m2;
    @DecimalMin("0") private BigDecimal max_area_m2;
    @Size(max=20) private String province_code;
    @Size(max=20) private List<@Positive Long> amenity_ids;
    @DecimalMin("-90") @DecimalMax("90") private Double latitude;
    @DecimalMin("-180") @DecimalMax("180") private Double longitude;
    @DecimalMin("0.1") @DecimalMax("100") private Double radius_km;
    @Pattern(regexp="newest|rent_asc|rent_desc|nearest") private String sort="newest";
    @Min(0) private int page=0;
    @Min(1) @Max(50) private int size=20;
    public void validateRanges() {
        if (min_rent_vnd!=null && max_rent_vnd!=null && min_rent_vnd.compareTo(max_rent_vnd)>0
                || min_area_m2!=null && max_area_m2!=null && min_area_m2.compareTo(max_area_m2)>0)
            throw new IllegalArgumentException("Minimum must not exceed maximum");
        if ((latitude==null)!=(longitude==null) || (radius_km!=null || "nearest".equals(sort)) && latitude==null)
            throw new IllegalArgumentException("latitude and longitude are required for geographic search");
        if (room_type!=null) com.homely.rental.catalog.entity.RoomType.valueOf(room_type);
    }
}
