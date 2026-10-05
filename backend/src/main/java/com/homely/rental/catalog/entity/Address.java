package com.homely.rental.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Address embedded entity for rooms.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_line", length = 300)
    private String line;

    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Column(name = "ward_code", length = 20)
    private String wardCode;

    @Column(name = "ward_name", length = 100)
    private String wardName;
}
