package com.brewbble.customer;

import com.brewbble.menu.MenuItem;
import com.brewbble.user.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_presets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    // Saved customization option IDs
    @ElementCollection
    @CollectionTable(name = "user_preset_options",
            joinColumns = @JoinColumn(name = "preset_id"))
    @Column(name = "option_id")
    @Builder.Default
    private List<Long> optionIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
