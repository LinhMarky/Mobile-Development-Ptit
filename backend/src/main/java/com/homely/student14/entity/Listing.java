package com.homely.student14.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Database record used by the booking service. SQL is kept in BookingRepository. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Listing {

    private Long id;
    private Long roomId;
    private BigDecimal rentVnd;
    private BigDecimal depositVnd;
    private Integer termsVersion;
    private String fees;
}
