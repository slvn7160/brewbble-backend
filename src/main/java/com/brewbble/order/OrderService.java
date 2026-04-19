package com.brewbble.order;

import com.brewbble.common.PagedResponse;
import com.brewbble.customization.CustomizationOption;
import com.brewbble.customization.CustomizationOptionRepository;
import com.brewbble.customization.CustomizationType;
import com.brewbble.customization.OrderItemCustomization;
import com.brewbble.menu.MenuItem;
import com.brewbble.menu.MenuItemRepository;
import com.brewbble.payment.PaymentStatus;
import com.brewbble.promotion.Promotion;
import com.brewbble.promotion.PromotionService;
import com.brewbble.reward.RewardService;
import com.brewbble.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("1.99");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("30.00");

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomizationOptionRepository customizationOptionRepository;
    private final RewardService rewardService;
    private final PromotionService promotionService;

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, AppUser user) {
        return executeOrder(request.getItems(), request.getNotes(), request.isRedeemPoints(), user, false, null);
    }

    /** Called by employees for in-store orders. customer may be null for guest purchases. */
    @Transactional
    public OrderResponse placeInstoreOrder(List<PlaceOrderRequest.OrderLineRequest> items,
                                           String notes, boolean redeemPoints,
                                           AppUser customer, String promoCode) {
        return executeOrder(items, notes, redeemPoints, customer, true, promoCode);
    }

    private OrderResponse executeOrder(List<PlaceOrderRequest.OrderLineRequest> items,
                                       String notes, boolean redeemPoints,
                                       AppUser targetUser, boolean inStore, String promoCode) {
        List<OrderItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (PlaceOrderRequest.OrderLineRequest line : items) {
            MenuItem menuItem = menuItemRepository.findById(line.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + line.getMenuItemId()));

            if (!menuItem.isAvailable()) {
                throw new IllegalArgumentException("Item is not available: " + menuItem.getName());
            }

            List<CustomizationOption> chosenOptions = resolveCustomizations(line.getCustomizationIds());
            BigDecimal priceDelta = chosenOptions.stream()
                    .map(CustomizationOption::getPriceDelta)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal effectiveUnitPrice = menuItem.getPrice().add(priceDelta)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal lineSubtotal = effectiveUnitPrice
                    .multiply(BigDecimal.valueOf(line.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .name(menuItem.getName())
                    .unitPrice(effectiveUnitPrice)
                    .quantity(line.getQuantity())
                    .subtotal(lineSubtotal)
                    .build();

            // Snapshot chosen customizations onto the order item
            List<OrderItemCustomization> customizations = chosenOptions.stream()
                    .map(opt -> OrderItemCustomization.builder()
                            .orderItem(orderItem)
                            .optionId(opt.getId())
                            .name(opt.getName())
                            .type(opt.getType())
                            .priceDelta(opt.getPriceDelta())
                            .build())
                    .collect(Collectors.toList());
            orderItem.getCustomizations().addAll(customizations);

            lines.add(orderItem);
            subtotal = subtotal.add(lineSubtotal);
        }

        // Apply promo discount to subtotal — tax is calculated on the discounted amount
        BigDecimal promoDiscount = BigDecimal.ZERO;
        Promotion appliedPromo = null;
        if (promoCode != null && !promoCode.isBlank()) {
            PromotionService.PromoResult result = promotionService.applyPromo(promoCode, subtotal);
            promoDiscount = result.discount();
            appliedPromo = result.promotion();
        }
        BigDecimal taxableAmount = subtotal.subtract(promoDiscount);
        BigDecimal tax = taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        // In-store orders have no delivery fee
        BigDecimal deliveryFee = inStore || subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                ? BigDecimal.ZERO : DELIVERY_FEE;
        BigDecimal total = taxableAmount.add(tax).add(deliveryFee);

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
                .promoDiscount(promoDiscount)
                .promotion(appliedPromo)
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

        // Points are earned only when the order is delivered — not at placement
        return OrderResponse.from(saved);
    }

    private List<CustomizationOption> resolveCustomizations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<CustomizationOption> options = customizationOptionRepository.findAllById(ids);
        if (options.size() != ids.size()) {
            throw new IllegalArgumentException("One or more customization options not found");
        }

        for (CustomizationOption opt : options) {
            if (!opt.isAvailable()) {
                throw new IllegalArgumentException("Customization option is not available: " + opt.getName());
            }
        }

        // At most 1 selection per non-TOPPING type
        Map<CustomizationType, Long> countByType = options.stream()
                .collect(Collectors.groupingBy(CustomizationOption::getType, Collectors.counting()));
        countByType.forEach((type, count) -> {
            if (type != CustomizationType.TOPPING && count > 1) {
                throw new IllegalArgumentException(
                        "Only one " + type.name().toLowerCase().replace('_', ' ') + " selection is allowed per item");
            }
        });

        return options;
    }

    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(OrderResponse::from).toList();
    }

    public PagedResponse<OrderResponse> getAllOrders(int page, int size, List<OrderStatus> statuses) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (statuses == null || statuses.isEmpty()) {
            return PagedResponse.from(orderRepository.findAll(pageable).map(OrderResponse::from));
        }
        return PagedResponse.from(orderRepository.findByStatusIn(statuses, pageable).map(OrderResponse::from));
    }

    public TodayRevenue getTodayRevenue() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal revenue = orderRepository.sumRevenueAfter(startOfDay);
        long count = orderRepository.countOrdersAfter(startOfDay);
        return new TodayRevenue(LocalDate.now(ZoneOffset.UTC).toString(), revenue, count);
    }

    public RevenueReport getRevenueSummary(LocalDate from, LocalDate toExclusive) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = toExclusive.atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalRevenue = orderRepository.sumRevenueBetween(fromInstant, toInstant);
        long orderCount         = orderRepository.countOrdersBetween(fromInstant, toInstant);

        List<DailyEntry> breakdown = orderRepository.dailyRevenueBetween(fromInstant, toInstant)
                .stream()
                .map(row -> new DailyEntry(
                        row[0].toString(),               // java.sql.Date → "yyyy-MM-dd"
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue()))
                .toList();

        return new RevenueReport(
                from.toString(),
                toExclusive.minusDays(1).toString(),
                totalRevenue, orderCount, breakdown);
    }

    public ShiftSummary getShiftSummary(LocalDate date) {
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal totalRevenue = orderRepository.sumRevenueBetween(from, to);
        long totalOrders        = orderRepository.countOrdersBetween(from, to);

        Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        for (Object[] row : orderRepository.countByStatusBetween(from, to)) {
            byStatus.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        Map<String, ShiftSummary.PaymentMethodEntry> byPaymentMethod = new java.util.LinkedHashMap<>();
        for (Object[] row : orderRepository.summaryByPaymentMethodBetween(from, to)) {
            byPaymentMethod.put(row[0].toString(),
                    new ShiftSummary.PaymentMethodEntry(
                            ((Number) row[1]).longValue(),
                            (BigDecimal) row[2]));
        }

        return new ShiftSummary(date.toString(), totalOrders, totalRevenue, byStatus, byPaymentMethod);
    }

    public record TodayRevenue(String date, BigDecimal revenue, long orderCount) {}
    public record DailyEntry(String date, BigDecimal revenue, long orderCount) {}
    public record RevenueReport(String from, String to, BigDecimal totalRevenue, long orderCount, List<DailyEntry> breakdown) {}
    public record ShiftSummary(
            String date,
            long totalOrders,
            BigDecimal totalRevenue,
            Map<String, Long> byStatus,
            Map<String, PaymentMethodEntry> byPaymentMethod) {
        public record PaymentMethodEntry(long count, BigDecimal revenue) {}
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);

        // Award points only when order reaches DELIVERED
        if (newStatus == OrderStatus.DELIVERED && saved.getUser() != null
                && saved.getPaymentStatus() == PaymentStatus.PAID) {
            int points = rewardService.earnPoints(saved.getUser(), saved.getId(), saved.getTotal());
            saved.setPointsEarned(points);
        }

        return OrderResponse.from(saved);
    }
}
