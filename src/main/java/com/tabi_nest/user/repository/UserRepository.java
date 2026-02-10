package com.tabi_nest.user.repository;

import com.tabi_nest.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    java.util.Optional<com.tabi_nest.user.domain.User> findByProviderAndProviderId(String provider, String providerId);
}
