package com.pfe.adminagent.user.repository;

import com.pfe.adminagent.user.domain.Role;
import com.pfe.adminagent.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleInAndActiveIsTrue(Collection<Role> roles);
}
