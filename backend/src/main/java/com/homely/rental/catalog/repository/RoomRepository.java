package com.homely.rental.catalog.repository;

import com.homely.rental.catalog.entity.Room;
import com.homely.rental.catalog.entity.RoomAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Room r where r.id = :id")
    Optional<Room> lockById(Long id);
    boolean existsByHostIdAndUnitCode(Long hostId, String unitCode);
    Optional<Room> findByIdAndHostId(Long id, Long hostId);
    Page<Room> findByHostId(Long hostId, Pageable pageable);
    Page<Room> findByHostIdAndAvailability(Long hostId, RoomAvailability availability, Pageable pageable);
}
