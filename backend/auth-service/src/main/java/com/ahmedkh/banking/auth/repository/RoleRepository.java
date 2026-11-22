package com.ahmedkh.banking.auth.repository;

import com.ahmedkh.banking.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
