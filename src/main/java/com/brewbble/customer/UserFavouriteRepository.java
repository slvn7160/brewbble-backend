package com.brewbble.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFavouriteRepository extends JpaRepository<UserFavourite, Long> {

    List<UserFavourite> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserFavourite> findByUserIdAndMenuItemId(Long userId, Long menuItemId);

    boolean existsByUserIdAndMenuItemId(Long userId, Long menuItemId);
}
