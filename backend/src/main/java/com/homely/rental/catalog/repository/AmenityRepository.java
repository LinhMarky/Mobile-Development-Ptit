package com.homely.rental.catalog.repository;

import com.homely.rental.catalog.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    List<Amenity> findByIdIn(Set<Long> ids);
    boolean existsByName(String name);
}
