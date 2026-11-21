package com.ahmedkh.banking.user.repository;

import com.ahmedkh.banking.user.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {

    Optional<CustomerProfile> findByAuthUserId(UUID authUserId);
}
