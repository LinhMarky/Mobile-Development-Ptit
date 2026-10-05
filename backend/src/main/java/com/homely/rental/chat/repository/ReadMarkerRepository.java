package com.homely.rental.chat.repository;

import com.homely.rental.chat.entity.ReadMarker;
import com.homely.rental.chat.entity.ReadMarkerId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadMarkerRepository extends JpaRepository<ReadMarker, ReadMarkerId> {
}
