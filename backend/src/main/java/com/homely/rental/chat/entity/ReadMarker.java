package com.homely.rental.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "read_markers")
@Getter
@Setter
public class ReadMarker {
    @EmbeddedId
    private ReadMarkerId id;

    @Column(name = "last_read_sequence", nullable = false)
    private long lastReadSequence;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
}
