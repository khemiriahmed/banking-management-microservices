package com.ahmedkh.banking.auth.repository;

import com.ahmedkh.banking.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserCredential, UUID> {

    Optional<UserCredential> findByEmail(String email);

    boolean existsByEmail(String email);
}
