package org.komponente.userservice.repository;

import org.komponente.userservice.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findAdminByUsernameAndPassword(String username, String password);
    Optional<Admin> findAdminByConfirmTempPassword(Long confirmTempPassword);
}

