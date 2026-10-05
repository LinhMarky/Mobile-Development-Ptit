package com.homely.rental.catalog.entity;

import com.homely.rental.common.entity.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Amenity entity for rooms (CAT14).
 * Represents a facility/feature that can be associated with rooms.
 */
@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "icon", length = 100)
    private String icon;

    @Column(name = "category", length = 50)
    private String category;
}
