package com.homely.rental.interaction.repository;

import com.homely.rental.interaction.entity.ViewingSlot;
import com.homely.rental.interaction.entity.ViewingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ViewingSlotRepository extends JpaRepository<ViewingSlot, Long> {
    @org.springframework.data.jpa.repository.Query("select s.room.id from ViewingSlot s where s.id = :id")
    java.util.Optional<Long> findRoomId(Long id);

    List<ViewingSlot> findByRoomIdAndStatusAndStartAtAfterOrderByStartAt(
            Long roomId, ViewingSlotStatus status, Instant after);

    List<ViewingSlot> findByHostIdAndStatusOrderByStartAt(Long hostId, ViewingSlotStatus status);
}
