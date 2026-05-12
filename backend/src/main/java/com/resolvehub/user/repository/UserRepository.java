package com.resolvehub.user.repository;

import com.resolvehub.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<User> findByOrganizationIdOrderByEmailAsc(UUID organizationId);
}
