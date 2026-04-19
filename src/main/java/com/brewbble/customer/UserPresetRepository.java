package com.brewbble.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPresetRepository extends JpaRepository<UserPreset, Long> {

    List<UserPreset> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserPreset> findByIdAndUserId(Long id, Long userId);
}
