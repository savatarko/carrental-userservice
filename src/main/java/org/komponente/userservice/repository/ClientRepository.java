package org.komponente.userservice.repository;

import org.komponente.userservice.domain.Admin;
import org.komponente.userservice.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findClientByUsernameAndPassword(String username, String password);

    Optional<Client> findClientByUsername(String username);

    Optional<Client> findClientByEmail(String email);

    Optional<Client> findClientByActivated(Long activated);
    Optional<Client> findClientByConfirmTempPassword(Long confirmTempPassword);
}
