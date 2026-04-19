package com.brewbble.settings;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "store_hours")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreHours {

    @Id
    @Column(name = "day_of_week", length = 10)
    private String dayOfWeek;   // MONDAY … SUNDAY

    private LocalTime openTime;

    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private boolean closed = false;

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
