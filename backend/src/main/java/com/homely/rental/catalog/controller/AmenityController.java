package com.homely.rental.catalog.controller;

import com.homely.rental.catalog.entity.Amenity;
import com.homely.rental.catalog.repository.AmenityRepository;
import com.homely.rental.common.annotation.ApiMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Amenity controller (CAT14).
 * Public endpoint for listing all available amenities.
 */
@RestController
@RequestMapping(path = "${apiPrefix}/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityRepository amenityRepository;

    @GetMapping
    @ApiMessage("List all amenities")
    public ResponseEntity<List<Amenity>> getAllAmenities() {
        return ResponseEntity.ok(amenityRepository.findAll());
    }

    @GetMapping("/{id}")
    @ApiMessage("Get amenity detail")
    public ResponseEntity<Amenity> getAmenity(@PathVariable Long id) {
        return ResponseEntity.ok(amenityRepository.findById(id)
                .orElseThrow(() -> new com.homely.rental.common.exception.ResourceNotFoundException("Amenity", id)));
    }
}
