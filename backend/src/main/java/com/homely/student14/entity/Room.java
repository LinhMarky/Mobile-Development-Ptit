package com.homely.student14.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/** Database record used by the booking service. SQL is kept in BookingRepository. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {

    private Long id;
    private Long hostId;
    private Integer maxOccupants;
    private String availability;
}
