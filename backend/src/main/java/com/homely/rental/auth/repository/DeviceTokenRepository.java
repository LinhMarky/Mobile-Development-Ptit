package com.homely.rental.auth.repository;

import com.homely.rental.auth.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    java.util.List<DeviceToken> findByUserIdAndActiveTrue(Long userId);
    @Modifying
    @Query("update DeviceToken d set d.active=false where (d.user.id <> :userId or d.installationId <> :installationId) and (d.installationId=:installationId or d.token=:token)")
    int deactivatePreviousOwner(Long userId, String installationId, String token);
    @Modifying
    @Query("update DeviceToken d set d.active=false where d.user.id=:userId and d.installationId=:installationId")
    int deactivateInstallation(Long userId, String installationId);
    Optional<DeviceToken> findByUserIdAndInstallationId(Long userId, String installationId);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false WHERE dt.user.id = ?1")
    int deactivateAllByUserId(Long userId);
}
