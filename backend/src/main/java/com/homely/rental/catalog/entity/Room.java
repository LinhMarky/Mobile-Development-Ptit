package com.homely.rental.catalog.entity;

import com.homely.rental.auth.entity.User;
import com.homely.rental.common.entity.AbstractAuditingEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Room entity (BR-01).
 * Represents a physical room owned by a host.
 * unit_code and host_id are immutable in v1.
 */
@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"host_id", "unit_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false, updatable = false)
    private User host;

    /** Immutable room code: [A-Z0-9_-]+ */
    @Column(name = "unit_code", length = 30, nullable = false, updatable = false)
    private String unitCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 20, nullable = false)
    private RoomType roomType;

    @Column(name = "area_m2", precision = 6, scale = 1, nullable = false)
    private BigDecimal areaM2;

    @Column(name = "max_occupants", nullable = false)
    private int maxOccupants = 1;

    @Embedded
    private Address address;

    @Embedded
    private GeoPoint location;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", length = 20, nullable = false)
    private RoomAvailability availability = RoomAvailability.AVAILABLE;

    @Column(name = "terms_version", nullable = false)
    private int termsVersion = 1;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "room_amenities",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new HashSet<>();

    @Version
    private int version;
}
