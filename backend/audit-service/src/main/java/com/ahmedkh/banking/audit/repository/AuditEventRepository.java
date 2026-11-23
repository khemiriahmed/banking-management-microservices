package com.ahmedkh.banking.audit.repository;

import com.ahmedkh.banking.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    boolean existsByEventId(UUID eventId);
    Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
