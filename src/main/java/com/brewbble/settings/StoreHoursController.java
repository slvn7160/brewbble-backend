package com.brewbble.settings;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/settings/store-hours")
@RequiredArgsConstructor
public class StoreHoursController {

    private final StoreHoursRepository storeHoursRepository;

    /** Public — frontend shows store hours to all visitors. */
    @GetMapping
    public ResponseEntity<List<StoreHours>> getAll() {
        return ResponseEntity.ok(storeHoursRepository.findAll());
    }

    /** ADMIN — update hours for one day. */
    @PutMapping("/{dayOfWeek}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoreHours> update(
            @PathVariable String dayOfWeek,
            @RequestBody StoreHoursRequest req) {

        String day = dayOfWeek.toUpperCase();
        StoreHours hours = storeHoursRepository.findById(day)
                .orElseThrow(() -> new IllegalArgumentException("Unknown day: " + day));

        hours.setClosed(req.isClosed());
        hours.setOpenTime(req.isClosed() ? null : LocalTime.parse(req.getOpenTime()));
        hours.setCloseTime(req.isClosed() ? null : LocalTime.parse(req.getCloseTime()));
        hours.setUpdatedAt(Instant.now());

        return ResponseEntity.ok(storeHoursRepository.save(hours));
    }

    @Data
    public static class StoreHoursRequest {
        private boolean closed;
        private String openTime;    // "HH:mm" — required when closed=false
        private String closeTime;   // "HH:mm" — required when closed=false
    }
}
