package com.brewbble.user;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Data
    @Builder
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String role;
        private Instant createdAt;

        public static UserResponse from(AppUser user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }
}
