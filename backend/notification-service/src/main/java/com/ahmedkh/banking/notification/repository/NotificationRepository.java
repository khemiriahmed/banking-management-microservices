package com.ahmedkh.banking.notification.repository;

import com.ahmedkh.banking.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByEventId(UUID eventId);
    Page<Notification> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId, Pageable pageable);
    Optional<Notification> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
