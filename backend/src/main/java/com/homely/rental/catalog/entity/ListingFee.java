package com.homely.rental.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Listing fee entity (BR-04).
 * Additional fees associated with a listing (electricity, water, internet, etc.)
 */
@Entity
@Table(name = "listing_fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListingFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "fee_code", length = 50, nullable = false)
    private String feeCode;

    @Column(name = "fee_mode", length = 20, nullable = false)
    private String feeMode = "FIXED";

    @Column(name = "amount_vnd", precision = 18, scale = 0, nullable = false)
    private BigDecimal amountVnd;

    @Column(name = "unit_name", length = 50)
    private String unitName; // e.g., "kWh", "m³", "month"

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
