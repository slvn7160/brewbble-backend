package com.brewbble.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE AppUser u SET u.rewardPoints = u.rewardPoints + :points WHERE u.id = :userId")
    void addPoints(@Param("userId") Long userId, @Param("points") int points);

    @Modifying
    @Query("UPDATE AppUser u SET u.rewardPoints = u.rewardPoints - :points WHERE u.id = :userId AND u.rewardPoints >= :points")
    int deductPoints(@Param("userId") Long userId, @Param("points") int points);
}
