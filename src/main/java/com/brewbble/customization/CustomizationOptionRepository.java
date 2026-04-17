package com.brewbble.customization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomizationOptionRepository extends JpaRepository<CustomizationOption, Long> {

    List<CustomizationOption> findByAvailableTrueOrderBySortOrderAsc();

    List<CustomizationOption> findByTypeAndAvailableTrueOrderBySortOrderAsc(CustomizationType type);
}
