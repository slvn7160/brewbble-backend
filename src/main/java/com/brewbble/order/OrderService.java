package com.brewbble.order;

import com.brewbble.menu.MenuItem;
import com.brewbble.menu.MenuItemRepository;
import com.brewbble.reward.RewardService;
import com.brewbble.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("1.99");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("30.00");

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RewardService rewardService;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, AppUser user) {
        return executeOrder(request.getItems(), request.getNotes(), request.isRedeemPoints(), user, false);
    }

    /** Called by employees for in-store orders. customer may be null for guest purchases. */
    @Transactional
    public OrderResponse placeInstoreOrder(List<PlaceOrderRequest.OrderLineRequest> items,
                                           String notes, boolean redeemPoints, AppUser customer) {
        return executeOrder(items, notes, redeemPoints, customer, true);
    }

    private OrderResponse executeOrder(List<PlaceOrderRequest.OrderLineRequest> items,
                                       String notes, boolean redeemPoints,
                                       AppUser targetUser, boolean inStore) {
        List<OrderItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (PlaceOrderRequest.OrderLineRequest line : items) {
            MenuItem menuItem = menuItemRepository.findById(line.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + line.getMenuItemId()));

            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("Item is not available: " + menuItem.getName());
            }

            BigDecimal lineSubtotal = menuItem.getPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            lines.add(OrderItem.builder()
                    .menuItem(menuItem)
                    .name(menuItem.getName())
                    .unitPrice(menuItem.getPrice())
                    .quantity(line.getQuantity())
                    .subtotal(lineSubtotal)
                    .build());

            subtotal = subtotal.add(lineSubtotal);
        }

        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        // In-store orders have no delivery fee
        BigDecimal deliveryFee = inStore || subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                ? BigDecimal.ZERO : DELIVERY_FEE;
        BigDecimal total = subtotal.add(tax).add(deliveryFee);

        // Apply reward redemption (only if a customer account is linked)
        BigDecimal rewardDiscount = BigDecimal.ZERO;
        if (redeemPoints && targetUser != null) {
            BigDecimal tentativeDiscount = rewardService.redeemPoints(targetUser, null);
            if (tentativeDiscount.compareTo(BigDecimal.ZERO) > 0) {
                rewardDiscount = tentativeDiscount.min(total);
                total = total.subtract(rewardDiscount).max(BigDecimal.ZERO);
            }
        }

        Order order = Order.builder()
                .user(targetUser)
                .subtotal(subtotal)
                .tax(tax)
                .deliveryFee(deliveryFee)
                .rewardDiscount(rewardDiscount)
                .total(total)
                .notes(notes)
                .build();

        lines.forEach(item -> item.setOrder(order));
        order.getItems().addAll(lines);

        Order saved = orderRepository.save(order);

        if (rewardDiscount.compareTo(BigDecimal.ZERO) > 0) {
            rewardService.updateLastRedemptionOrderId(targetUser.getId(), saved.getId());
        }

        // Earn points for the linked customer (online or in-store)
        int pointsEarned = 0;
        if (targetUser != null) {
            pointsEarned = rewardService.earnPoints(targetUser, saved.getId(), saved.getTotal());
        }
        saved.setPointsEarned(pointsEarned);

        return OrderResponse.from(saved);
    }

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(OrderResponse::from).toList();
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    public TodayRevenue getTodayRevenue() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal revenue = orderRepository.sumRevenueAfter(startOfDay);
        long count = orderRepository.countOrdersAfter(startOfDay);
        return new TodayRevenue(LocalDate.now(ZoneOffset.UTC).toString(), revenue, count);
    }

    public record TodayRevenue(String date, BigDecimal revenue, long orderCount) {}

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        return OrderResponse.from(orderRepository.save(order));
    }
}
