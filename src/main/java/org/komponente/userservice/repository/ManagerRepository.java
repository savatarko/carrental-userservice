package org.komponente.userservice.repository;

import org.komponente.userservice.domain.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {
    Optional<Manager> findManagerByUsernameAndPassword(String username, String password);

    Optional<Manager> findManagerByUsername(String username);

    //Optional<Manager> findManagerByEmail(String email);

    Optional<Manager> findManagerByActivated(Long activated);
}
