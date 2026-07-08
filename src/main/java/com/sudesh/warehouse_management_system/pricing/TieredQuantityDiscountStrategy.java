package com.sudesh.warehouse_management_system.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TieredQuantityDiscountStrategy implements PricingStrategy {

    private static final BigDecimal TIER_1_DISCOUNT = BigDecimal.ZERO;              // < 10 units
    private static final BigDecimal TIER_2_DISCOUNT = BigDecimal.valueOf(5);         // 10-49 units
    private static final BigDecimal TIER_3_DISCOUNT = BigDecimal.valueOf(10);        // 50-99 units
    private static final BigDecimal TIER_4_DISCOUNT = BigDecimal.valueOf(15);        // 100+ units

    @Override
    public PricingResult calculatePrice(PricingContext context) {
        BigDecimal unitPrice = context.getUnitPrice();
        int quantity = context.getQuantity();

        BigDecimal originalTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountPercentage = resolveDiscountPercentage(quantity);

        BigDecimal discountAmount = originalTotal
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal finalTotal = originalTotal.subtract(discountAmount);

        return PricingResult.builder()
                .originalTotal(originalTotal)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .build();
    }

    private BigDecimal resolveDiscountPercentage(int quantity) {
        if (quantity >= 100) {
            return TIER_4_DISCOUNT;
        } else if (quantity >= 50) {
            return TIER_3_DISCOUNT;
        } else if (quantity >= 10) {
            return TIER_2_DISCOUNT;
        } else {
            return TIER_1_DISCOUNT;
        }
    }
}