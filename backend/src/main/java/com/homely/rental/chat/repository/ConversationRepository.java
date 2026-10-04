package com.homely.rental.chat.repository;

import com.homely.rental.chat.entity.Conversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByRoomIdAndTenantId(Long roomId, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findLockedById(@Param("id") Long id);

    @Query("select c from Conversation c where c.tenant.id = :userId or c.host.id = :userId " +
            "order by coalesce(c.lastMessageAt, c.createdAt) desc, c.id desc")
    Page<Conversation> findForMember(@Param("userId") Long userId, Pageable pageable);
}
