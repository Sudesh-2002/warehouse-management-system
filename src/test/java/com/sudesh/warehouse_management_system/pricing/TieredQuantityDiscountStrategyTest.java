package com.sudesh.warehouse_management_system.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TieredQuantityDiscountStrategyTest {

    private final TieredQuantityDiscountStrategy strategy = new TieredQuantityDiscountStrategy();

    @ParameterizedTest(name = "quantity={0} -> expected discount={1}%")
    @CsvSource({
            "1,   0",
            "9,   0",
            "10,  5",
            "49,  5",
            "50,  10",
            "99,  10",
            "100, 15",
            "500, 15"
    })
    void calculatePrice_shouldApplyCorrectDiscountTier(int quantity, String expectedDiscount) {
        PricingContext context = PricingContext.builder()
                .unitPrice(BigDecimal.TEN)
                .quantity(quantity)
                .build();

        PricingResult result = strategy.calculatePrice(context);

        assertThat(result.getDiscountPercentage())
                .isEqualByComparingTo(new BigDecimal(expectedDiscount));
    }

    @Test
    void calculatePrice_shouldCalculateCorrectFinalTotal_forBulkTier() {
        // 100 units at $10 each = $1000 original, 15% discount = $150 off -> $850 final
        PricingContext context = PricingContext.builder()
                .unitPrice(BigDecimal.TEN)
                .quantity(100)
                .build();

        PricingResult result = strategy.calculatePrice(context);

        assertThat(result.getOriginalTotal()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(result.getFinalTotal()).isEqualByComparingTo(BigDecimal.valueOf(850.00));
    }

    @Test
    void calculatePrice_shouldApplyNoDiscount_forSmallQuantity() {
        // 5 units at $12.50 each = $62.50, no discount
        PricingContext context = PricingContext.builder()
                .unitPrice(BigDecimal.valueOf(12.50))
                .quantity(5)
                .build();

        PricingResult result = strategy.calculatePrice(context);

        assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getFinalTotal()).isEqualByComparingTo(result.getOriginalTotal());
    }
}