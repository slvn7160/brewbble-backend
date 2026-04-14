package com.brewbble.reward;

import com.brewbble.user.AppUser;
import com.brewbble.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardService {

    public static final int    REDEEM_THRESHOLD  = 100;           // points needed for a free drink
    public static final BigDecimal REDEEM_DISCOUNT = new BigDecimal("10.00"); // $ value of free drink

    private final UserRepository              userRepository;
    private final RewardTransactionRepository rewardTxRepository;

    /** Award 1 point per $1 of the order total. Call after order is persisted. */
    @Transactional
    public int earnPoints(AppUser user, Long orderId, BigDecimal orderTotal) {
        int points = orderTotal.setScale(0, RoundingMode.FLOOR).intValue();
        if (points <= 0) return 0;

        userRepository.addPoints(user.getId(), points);

        rewardTxRepository.save(RewardTransaction.builder()
                .user(user)
                .orderId(orderId)
                .type(RewardTransaction.RewardType.EARNED)
                .points(points)
                .build());

        return points;
    }

    /**
     * Attempt to redeem 100 points for a $10 discount.
     * Returns the discount amount, or ZERO if user doesn't have enough points.
     */
    @Transactional
    public BigDecimal redeemPoints(AppUser user, Long orderId) {
        int updated = userRepository.deductPoints(user.getId(), REDEEM_THRESHOLD);
        if (updated == 0) {
            return BigDecimal.ZERO; // not enough points
        }

        rewardTxRepository.save(RewardTransaction.builder()
                .user(user)
                .orderId(orderId)
                .type(RewardTransaction.RewardType.REDEEMED)
                .points(REDEEM_THRESHOLD)
                .build());

        return REDEEM_DISCOUNT;
    }

    @Transactional
    public void updateLastRedemptionOrderId(Long userId, Long orderId) {
        rewardTxRepository.linkLatestRedemption(userId, orderId);
    }

    public RewardSummary getSummary(AppUser user) {
        AppUser fresh = userRepository.findById(user.getId()).orElseThrow();
        int balance = fresh.getRewardPoints();
        List<RewardTransaction> txs = rewardTxRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return new RewardSummary(balance, REDEEM_THRESHOLD, txs);
    }

    public record RewardSummary(int points, int freedrinkThreshold, List<RewardTransaction> transactions) {}
}
