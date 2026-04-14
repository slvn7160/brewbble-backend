package com.brewbble.reward;

import com.brewbble.user.AppUser;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/me")
    public ResponseEntity<RewardResponse> myRewards(@AuthenticationPrincipal AppUser user) {
        RewardService.RewardSummary summary = rewardService.getSummary(user);
        return ResponseEntity.ok(RewardResponse.from(summary));
    }

    @Data
    @Builder
    public static class RewardResponse {
        private int points;
        private int freedrinkThreshold;
        private int pointsToFreeDrink;
        private List<TransactionDto> transactions;

        public static RewardResponse from(RewardService.RewardSummary summary) {
            List<TransactionDto> txs = summary.transactions().stream()
                    .map(tx -> TransactionDto.builder()
                            .type(tx.getType().name())
                            .points(tx.getPoints())
                            .orderId(tx.getOrderId())
                            .createdAt(tx.getCreatedAt())
                            .build())
                    .toList();

            int toFree = Math.max(0, summary.freedrinkThreshold() - summary.points());
            return RewardResponse.builder()
                    .points(summary.points())
                    .freedrinkThreshold(summary.freedrinkThreshold())
                    .pointsToFreeDrink(toFree)
                    .transactions(txs)
                    .build();
        }
    }

    @Data
    @Builder
    public static class TransactionDto {
        private String type;
        private int points;
        private Long orderId;
        private Instant createdAt;
    }
}
