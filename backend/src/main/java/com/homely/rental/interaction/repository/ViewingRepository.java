package com.homely.rental.interaction.repository;

import com.homely.rental.interaction.entity.Viewing;
import com.homely.rental.interaction.entity.ViewingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewingRepository extends JpaRepository<Viewing, Long> {
    @org.springframework.data.jpa.repository.Query("select v.room.id from Viewing v where v.id = :id")
    java.util.Optional<Long> findRoomId(Long id);

    Page<Viewing> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<Viewing> findByHostIdOrderByCreatedAtDesc(Long hostId, Pageable pageable);

    boolean existsByTenantIdAndRoomIdAndStatusIn(Long tenantId, Long roomId, List<ViewingStatus> statuses);
}
